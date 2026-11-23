export { cn } from "cn"


// Import all default avatars
const avatars = import.meta.glob<string>(
  "@/assets/default-avatar/*.svg",
  {
    eager: true,
    query: "?url",
    import: "default",
  }
);

export function getDefaultAvatar(filename: string | null): string | null {
  if (!filename) {
    return null;
  }

  const photo = avatars[`/src/assets/default-avatar/${filename}.svg`];

  if (!photo) {
    return filename;
  }

  return photo;
}

export function interpolateColor(
  start: string,
  end: string,
  factor: number
) {
  return `color-mix(in oklch, ${start} ${((1 - factor) * 100).toFixed(2)}%, ${end})`;
}
