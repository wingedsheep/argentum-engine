import flyingIcon from './flying.svg'
import reachIcon from './reach.svg'
import trampleIcon from './trample.svg'
import firstStrikeIcon from './first-strike.svg'
import deathtouchIcon from './deathtouch.svg'
import lifelinkIcon from './lifelink.svg'
import vigilanceIcon from './vigilance.svg'
import hasteIcon from './haste.svg'
import hexproofIcon from './hexproof.svg'
import indestructibleIcon from './indestructible.svg'
import defenderIcon from './defender.svg'
import menaceIcon from './menace.svg'
import protectionIcon from './protection.svg'
import genericIcon from './generic.svg'

export const keywordIcons: Record<string, string> = {
  FLYING: flyingIcon,
  REACH: reachIcon,
  TRAMPLE: trampleIcon,
  FIRST_STRIKE: firstStrikeIcon,
  DOUBLE_STRIKE: firstStrikeIcon,
  DEATHTOUCH: deathtouchIcon,
  LIFELINK: lifelinkIcon,
  VIGILANCE: vigilanceIcon,
  HASTE: hasteIcon,
  HEXPROOF: hexproofIcon,
  SHROUD: hexproofIcon,
  INDESTRUCTIBLE: indestructibleIcon,
  DEFENDER: defenderIcon,
  MENACE: menaceIcon,
}

export const genericKeywordIcon = genericIcon

/** Exported for use in CardOverlays colored protection icons */
export { protectionIcon }

export const displayableKeywords = new Set([
  'FLYING', 'REACH', 'TRAMPLE',
  'FIRST_STRIKE', 'DOUBLE_STRIKE', 'DEATHTOUCH',
  'LIFELINK', 'VIGILANCE', 'HASTE', 'HEXPROOF',
  'SHROUD', 'INDESTRUCTIBLE', 'DEFENDER', 'MENACE',
])
