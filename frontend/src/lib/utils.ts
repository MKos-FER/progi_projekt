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

export function getDefaultAvatar(filename: string): string {
  const photo = avatars[`/src/assets/default-avatar/${filename}.svg`];

  if (!photo) {
    throw new Error(`Avatar ${filename} not found!`);
  }

  return photo;
}
