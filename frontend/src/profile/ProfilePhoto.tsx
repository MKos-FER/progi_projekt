import { useEffect, useRef, useState } from 'react'
import type { ChangeEvent } from 'react'
import './ProfilePhoto.css'

type PhotoResponse = {
  profilePhotoUrl: string | null
  csrfToken: string | null
  csrfHeaderName: string | null
}

const MAX_BYTES = 5 * 1024 * 1024

function requestHeaders(): Headers {
  const headers = new Headers()
  // Temporary compatibility with the project's development identity resolver.
  const userId: unknown = import.meta.env.VITE_PROFILE_DEV_USER_ID
  if (import.meta.env.DEV && typeof userId === 'string' && userId) {
    headers.set('X-User-Id', userId)
  }
  return headers
}

async function readResponse(response: Response): Promise<PhotoResponse> {
  const body: unknown = await response.json().catch(() => null)
  if (!response.ok) {
    if (response.status === 401) throw new Error('Za uređivanje fotografije prijavite se u svoj profil.')
    if (response.status === 413) throw new Error('Fotografija smije imati najviše 5 MiB.')
    if (body && typeof body === 'object' && 'message' in body && typeof body.message === 'string') {
      throw new Error(body.message)
    }
    throw new Error('Fotografiju nije moguće spremiti ili dohvatiti. Pokušajte ponovno.')
  }
  if (!body || typeof body !== 'object' || !('profilePhotoUrl' in body)
    || (body.profilePhotoUrl !== null && typeof body.profilePhotoUrl !== 'string')) {
    throw new Error('Profil trenutačno nije dostupan.')
  }
  return {
    profilePhotoUrl: body.profilePhotoUrl,
    csrfToken: 'csrfToken' in body && typeof body.csrfToken === 'string' ? body.csrfToken : null,
    csrfHeaderName: 'csrfHeaderName' in body && typeof body.csrfHeaderName === 'string' ? body.csrfHeaderName : null,
  }
}

export default function ProfilePhoto() {
  const [photo, setPhoto] = useState<PhotoResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [brokenImage, setBrokenImage] = useState(false)
  const input = useRef<HTMLInputElement>(null)

  useEffect(() => {
    const abort = new AbortController()
    async function load() {
      try {
        const response = await fetch('/api/v1/users/me/photo', {
          credentials: 'same-origin', headers: requestHeaders(), signal: abort.signal,
        })
        const value = await readResponse(response)
        if (!abort.signal.aborted) setPhoto(value)
      } catch (cause) {
        if (!abort.signal.aborted) setError(cause instanceof Error ? cause.message : 'Profil nije dostupan.')
      } finally {
        if (!abort.signal.aborted) setLoading(false)
      }
    }
    void load()
    return () => { abort.abort() }
  }, [])

  async function upload(event: ChangeEvent<HTMLInputElement>) {
    const file = event.currentTarget.files?.[0]
    event.currentTarget.value = ''
    if (!file || !photo || saving) return
    setError('')
    setNotice('')
    if (!['image/png', 'image/jpeg'].includes(file.type)) {
      setError('Odaberite JPEG ili PNG fotografiju.')
      return
    }
    if (file.size === 0 || file.size > MAX_BYTES) {
      setError('Odaberite nepraznu fotografiju do 5 MiB.')
      return
    }
    const data = new FormData()
    data.append('file', file)
    const headers = requestHeaders()
    if (photo.csrfHeaderName && photo.csrfToken) headers.set(photo.csrfHeaderName, photo.csrfToken)
    setSaving(true)
    try {
      const response = await fetch('/api/v1/users/me/photo', {
        method: 'PUT', credentials: 'same-origin', headers, body: data,
      })
      setPhoto(await readResponse(response))
      setBrokenImage(false)
      setNotice('Profilna fotografija je spremljena.')
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Fotografiju nije moguće spremiti.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="profile-photo" aria-labelledby="profile-photo-heading">
      <h2 id="profile-photo-heading">Profilna fotografija</h2>
      {photo?.profilePhotoUrl && !brokenImage
        ? <img className="profile-photo-avatar" src={photo.profilePhotoUrl} alt="Vaša profilna fotografija" onError={() => { setBrokenImage(true) }} />
        : <div className="profile-photo-avatar profile-photo-placeholder" role="img" aria-label="Profil bez fotografije">👤</div>}
      <p>Fotografija je opcionalna. JPEG ili PNG do 5 MiB, najviše 4096 × 4096 piksela.</p>
      <input ref={input} type="file" accept="image/jpeg,image/png" aria-label="Odaberite profilnu fotografiju"
        hidden disabled={loading || saving || !photo} onChange={(event) => { void upload(event) }} />
      <button type="button" disabled={loading || saving || !photo} onClick={() => { input.current?.click() }}>
        {loading ? 'Učitavanje…' : saving ? 'Spremanje…' : photo?.profilePhotoUrl ? 'Zamijeni fotografiju' : 'Postavi fotografiju'}
      </button>
      <p role="status">{notice}</p>
      {error && <p role="alert">{error}</p>}
    </section>
  )
}
