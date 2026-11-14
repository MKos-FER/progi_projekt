import { useEditUserProfile, useMyUserProfile } from "@/apiStore/useUsers"


export default function EditProfile() {
    return(
        <>
            Edit profile
        </>
    );
}

export function EditPicture(){
    const {mutate: editUserProfile, isPending, error: editError} = useEditUserProfile();

    const {data: user, isLoading, error} = useMyUserProfile();

    
}