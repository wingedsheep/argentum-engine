/**
 * Draws an avatar's picture into its parent — which must be `position: relative` and sized — clipped
 * to a circle: a preset portrait fills it, a card-art avatar shows its crop of the illustration.
 * Renders nothing for no avatar, so callers fall back to the initial.
 */
import type React from 'react'
import { type ResolvedAvatar, resolveAvatar } from './avatars'
import s from './AvatarArt.module.css'

export function AvatarArt({ avatar }: { avatar: string | ResolvedAvatar | null | undefined }) {
  const resolved = typeof avatar === 'string' || avatar == null ? resolveAvatar(avatar) : avatar
  if (!resolved) return null
  return (
    <span className={s.art} aria-hidden>
      {resolved.kind === 'preset' ? (
        <img className={s.preset} src={resolved.url} alt="" draggable={false} />
      ) : (
        <img className={s.crop} src={resolved.url} alt="" draggable={false} style={cropStyle(resolved.crop)} />
      )}
    </span>
  )
}

/** Scale the art so the crop's side fills the circle, then shift its corner to the circle's. */
export function cropStyle(crop: { x: number; y: number; size: number }): React.CSSProperties {
  return {
    height: `${100 / crop.size}%`,
    left: `${(-crop.x / crop.size) * 100}%`,
    top: `${(-crop.y / crop.size) * 100}%`,
  }
}

/** True when the value draws a picture (rather than falling back to the initial). */
export function hasAvatarArt(avatar: string | null | undefined): boolean {
  return resolveAvatar(avatar) !== undefined
}
