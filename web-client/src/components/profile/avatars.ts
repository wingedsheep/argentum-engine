/**
 * Avatars: either one of the preset portraits, or a circle cut from a card's art.
 *
 * Presets — the server stores only the id (and accepts only the ids listed in
 * `game-server/.../profile/Avatars.kt`); the art ships here as `src/assets/avatars/<id>.webp`.
 * Adding a portrait means adding it in both places.
 *
 * Card art — `card:<x>,<y>,<size>:<path>`, where `path` is a printing's Scryfall image path
 * (`front/a/b/<uuid>`) and the crop square is measured in units of the art's height: `x`, `y` its
 * top-left corner, `size` its side. The server checks the path belongs to a catalogued printing.
 */
const images = import.meta.glob<string>('../../assets/avatars/*.webp', { eager: true, import: 'default' })

export interface AvatarOption {
  readonly id: string
  readonly name: string
  /** The colour of the portrait's group — the picker's hover glow and preview tint. */
  readonly tint: string
}

export interface AvatarGroup {
  readonly label: string
  /** Colour of the group's label dot. */
  readonly color: string
  readonly avatars: readonly AvatarOption[]
}

const group = (label: string, color: string, avatars: [string, string, string][]): AvatarGroup => ({
  label,
  color,
  avatars: avatars
    .map(([id, name, tint]) => ({ id, name, tint }))
    .filter((a) => images[`../../assets/avatars/${a.id}.webp`] !== undefined),
})

/** The picker's sections, one per colour, artifacts and wanderers last. */
/** The picker's sections — the creature types of each colour, then the wider Multiverse and artifacts. */
export const AVATAR_GROUPS: readonly AvatarGroup[] = [
  group('White', '#f3e6bf', [
    ['angel-sentinel', 'Angel Sentinel', '#f3d48a'],
    ['kithkin-skirmisher', 'Kithkin Skirmisher', '#f3d48a'],
    ['loxodon-hierarch', 'Loxodon Hierarch', '#f3d48a'],
    ['leonin-warden', 'Leonin Warden', '#f3d48a'],
    ['aven-windcaller', 'Aven Windcaller', '#f3d48a'],
    ['kor-skyclimber', 'Kor Skyclimber', '#f3d48a'],
    ['cathar-inquisitor', 'Cathar Inquisitor', '#f3d48a'],
    ['dawn-archon', 'Dawn Archon', '#f3d48a'],
  ]),
  group('Blue', '#6aa6e8', [
    ['storm-archmage', 'Storm Archmage', '#5d8fe6'],
    ['sphinx-sage', 'Sphinx Sage', '#5d8fe6'],
    ['merfolk-trickster', 'Merfolk Trickster', '#5d8fe6'],
    ['vedalken-artificer', 'Vedalken Artificer', '#5d8fe6'],
    ['faerie-rogue', 'Faerie Rogue', '#5d8fe6'],
    ['kraken-lord', 'Kraken Lord', '#5d8fe6'],
    ['djinn-wanderer', 'Djinn', '#5d8fe6'],
    ['moonfolk-seer', 'Moonfolk Seer', '#5d8fe6'],
  ]),
  group('Black', '#a58bc4', [
    ['vampire-aristocrat', 'Vampire Aristocrat', '#9a6be0'],
    ['zombie-ghoul', 'Zombie Ghoul', '#9a6be0'],
    ['lich-lord', 'Lich Lord', '#9a6be0'],
    ['demon-tyrant', 'Demon Tyrant', '#9a6be0'],
    ['nezumi-rogue', 'Rat Rogue', '#9a6be0'],
    ['specter-rider', 'Specter', '#9a6be0'],
    ['gorgon-recluse', 'Gorgon', '#9a6be0'],
    ['phyrexian-horror', 'Phyrexian Horror', '#9a6be0'],
  ]),
  group('Red', '#ef7a55', [
    ['goblin-guide', 'Goblin Guide', '#f0703a'],
    ['dragon-tyrant', 'Dragon Tyrant', '#f0703a'],
    ['viashino-pyromancer', 'Viashino Pyromancer', '#f0703a'],
    ['minotaur-berserker', 'Minotaur Berserker', '#f0703a'],
    ['ogre-warlord', 'Ogre Warlord', '#f0703a'],
    ['devil-trickster', 'Devil', '#f0703a'],
    ['phoenix-firebird', 'Phoenix', '#f0703a'],
    ['dwarf-forgemaster', 'Dwarf Forgemaster', '#f0703a'],
  ]),
  group('Green', '#6cc287', [
    ['elf-druid', 'Elf Druid', '#5cc278'],
    ['treefolk-elder', 'Treefolk Elder', '#5cc278'],
    ['centaur-courser', 'Centaur', '#5cc278'],
    ['hydra-matriarch', 'Hydra', '#5cc278'],
    ['werewolf-howler', 'Werewolf', '#5cc278'],
    ['dryad-arbor', 'Dryad', '#5cc278'],
    ['rhox-monk', 'Rhox Monk', '#5cc278'],
    ['squirrel-forager', 'Squirrel Forager', '#5cc278'],
  ]),
  group('Across the Multiverse', '#d9b96a', [
    ['kitsune-mystic', 'Kitsune Mystic', '#e0a85a'],
    ['naga-oracle', 'Naga Oracle', '#e0a85a'],
    ['satyr-reveler', 'Satyr Reveler', '#e0a85a'],
    ['giant-shaman', 'Giant Shaman', '#e0a85a'],
    ['orc-warlord', 'Orc Warlord', '#e0a85a'],
    ['raptor-rider', 'Dinosaur', '#e0a85a'],
    ['kami-spirit', 'Kami', '#e0a85a'],
    ['ninja-shadow', 'Ninja', '#e0a85a'],
  ]),
  group('Artifacts & Eldritch', '#c8ccd4', [
    ['artificer-golem', 'Golem', '#b9c3d1'],
    ['myr-sentinel', 'Myr', '#b9c3d1'],
    ['thopter-swarm', 'Thopter', '#b9c3d1'],
    ['gargoyle-sentinel', 'Gargoyle', '#b9c3d1'],
    ['construct-colossus', 'Construct', '#b9c3d1'],
    ['eldrazi-titan', 'Eldrazi', '#b9c3d1'],
    ['sliver-hive', 'Sliver', '#b9c3d1'],
    ['crystal-elemental', 'Elemental', '#b9c3d1'],
  ]),
].filter((g) => g.avatars.length > 0)

/** Every preset, in picker order. */
export const AVATARS: readonly AvatarOption[] = AVATAR_GROUPS.flatMap((g) => g.avatars)

const byId = new Map(AVATARS.map((a) => [a.id, a]))

export function avatarOption(id: string | null | undefined): AvatarOption | undefined {
  return id ? byId.get(id) : undefined
}

// ── Card art ────────────────────────────────────────────────────────────────

export interface CardCrop {
  readonly x: number
  readonly y: number
  readonly size: number
}

const CARD_PREFIX = 'card:'
const ART_PATH = /^(front|back)\/[0-9a-f]\/[0-9a-f]\/[0-9a-f-]{36}$/
const SCRYFALL_IMAGE = /^https:\/\/cards\.scryfall\.io\/[a-z_]+\/((?:front|back)\/[0-9a-f]\/[0-9a-f]\/[0-9a-f-]{36})\.\w+(\?.*)?$/

/** Smallest crop the server accepts, as a fraction of the art's height. */
export const MIN_CROP_SIZE = 0.15

/** A printing's Scryfall image path from its `imageUri`, or undefined when it isn't on the CDN. */
export function cardArtPath(imageUri: string | null | undefined): string | undefined {
  return imageUri?.match(SCRYFALL_IMAGE)?.[1]
}

/** The landscape illustration (no frame) for an image path. */
export function cardArtUrl(path: string): string {
  return `https://cards.scryfall.io/art_crop/${path}.jpg`
}

export function formatCardAvatar(path: string, crop: CardCrop): string {
  const n = (v: number) => v.toFixed(3)
  return `${CARD_PREFIX}${n(crop.x)},${n(crop.y)},${n(crop.size)}:${path}`
}

export type ResolvedAvatar =
  | { readonly kind: 'preset'; readonly url: string; readonly option: AvatarOption }
  | { readonly kind: 'card'; readonly url: string; readonly path: string; readonly crop: CardCrop }

/** What an avatar value draws, or undefined for none / a value this client can't draw. */
export function resolveAvatar(value: string | null | undefined): ResolvedAvatar | undefined {
  if (!value) return undefined
  if (value.startsWith(CARD_PREFIX)) {
    const rest = value.slice(CARD_PREFIX.length)
    const colon = rest.indexOf(':')
    if (colon < 0) return undefined
    const path = rest.slice(colon + 1)
    const [x, y, size] = rest.slice(0, colon).split(',').map(Number)
    if (!ART_PATH.test(path) || [x, y, size].some((v) => v === undefined || !Number.isFinite(v))) return undefined
    return { kind: 'card', url: cardArtUrl(path), path, crop: { x: x!, y: y!, size: size! } }
  }
  const option = byId.get(value)
  const url = images[`../../assets/avatars/${value}.webp`]
  return option && url ? { kind: 'preset', url, option } : undefined
}

/** A display name's initial — the avatar shown when nothing is picked. */
export function initialOf(name: string): string {
  return name.trim().charAt(0).toUpperCase() || '?'
}
