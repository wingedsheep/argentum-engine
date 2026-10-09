package com.wingedsheep.gameserver.profile

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.PrintingRegistry
import org.springframework.stereotype.Component
import java.util.Locale

/**
 * The preset avatar portraits a player can pick for their account. The server stores and relays only
 * the id; the images ship with the web client (`web-client/src/assets/avatars/<id>.webp`, catalogued
 * in `web-client/src/components/profile/avatars.ts`). Adding a portrait means adding it in both places —
 * the client offers what it has art for, the server accepts only what is listed here.
 */
object Avatars {
    val ids: Set<String> = linkedSetOf(
        // White
        "angel-sentinel", "kithkin-skirmisher", "loxodon-hierarch", "leonin-warden",
        "aven-windcaller", "kor-skyclimber", "cathar-inquisitor", "dawn-archon",
        // Blue
        "storm-archmage", "sphinx-sage", "merfolk-trickster", "vedalken-artificer",
        "faerie-rogue", "kraken-lord", "djinn-wanderer", "moonfolk-seer",
        // Black
        "vampire-aristocrat", "zombie-ghoul", "lich-lord", "demon-tyrant",
        "nezumi-rogue", "specter-rider", "gorgon-recluse", "phyrexian-horror",
        // Red
        "goblin-guide", "dragon-tyrant", "viashino-pyromancer", "minotaur-berserker",
        "ogre-warlord", "devil-trickster", "phoenix-firebird", "dwarf-forgemaster",
        // Green
        "elf-druid", "treefolk-elder", "centaur-courser", "hydra-matriarch",
        "werewolf-howler", "dryad-arbor", "rhox-monk", "squirrel-forager",
        // Across the Multiverse
        "kitsune-mystic", "naga-oracle", "satyr-reveler", "giant-shaman",
        "orc-warlord", "raptor-rider", "kami-spirit", "ninja-shadow",
        // Artifacts & Eldritch
        "artificer-golem", "myr-sentinel", "thopter-swarm", "gargoyle-sentinel",
        "construct-colossus", "eldrazi-titan", "sliver-hive", "crystal-elemental",
    )

    fun isPreset(id: String): Boolean = id in ids
}

/**
 * A circle cut from a card's illustration: `card:<x>,<y>,<size>:<path>`. [path] is the Scryfall image
 * path shared by every size of one printing's image (`front/a/b/<uuid>`); the client draws the
 * `art_crop` of it. The crop square is measured in units of the art's height — [x], [y] its top-left
 * corner, [size] its side — so it means the same thing whatever size the image is served at.
 */
data class CardArtAvatar(val x: Double, val y: Double, val size: Double, val path: String) {
    companion object {
        const val PREFIX = "card:"
        private val PATH = Regex("""^(front|back)/[0-9a-f]/[0-9a-f]/[0-9a-f-]{36}$""")
        private val IMAGE_URI = Regex("""^https://cards\.scryfall\.io/[a-z_]+/((?:front|back)/[0-9a-f]/[0-9a-f]/[0-9a-f-]{36})\.\w+(\?.*)?$""")

        /** Smallest crop: a sixth of the art's height is already a close-up of a face. */
        const val MIN_SIZE = 0.15
        /** Art crops run up to about 1.5:1, so the crop's left edge never needs to pass this. */
        private const val MAX_RIGHT = 1.6

        fun parse(value: String): CardArtAvatar? {
            if (!value.startsWith(PREFIX) || value.length > 120) return null
            val parts = value.removePrefix(PREFIX).split(':', limit = 2)
            if (parts.size != 2 || !PATH.matches(parts[1])) return null
            val nums = parts[0].split(',').map { it.toDoubleOrNull() ?: return null }
            if (nums.size != 3) return null
            val (x, y, size) = nums
            if (size !in MIN_SIZE..1.0 || x < 0 || y < 0 || y + size > 1.0001 || x + size > MAX_RIGHT) return null
            return CardArtAvatar(x, y, size, parts[1])
        }

        /** The image path of a `cards.scryfall.io` URL, or null for anything else. */
        fun pathOf(imageUri: String?): String? = imageUri?.let { IMAGE_URI.matchEntire(it)?.groupValues?.get(1) }

        fun format(x: Double, y: Double, size: Double, path: String): String =
            "$PREFIX${"%.3f".format(Locale.ROOT, x)},${"%.3f".format(Locale.ROOT, y)},${"%.3f".format(Locale.ROOT, size)}:$path"
    }
}

/**
 * Decides whether a stored avatar value is one the server will accept: a preset id, or a crop of the
 * art of a printing the card catalog actually has — never an arbitrary image.
 */
@Component
class AvatarValidator(
    private val cardRegistry: CardRegistry,
    private val printingRegistry: PrintingRegistry,
) {
    /** Every printing's image path, front and back. The catalog is fixed for the server's lifetime. */
    private val knownArtPaths: Set<String> by lazy {
        buildSet {
            for (name in cardRegistry.allCardNames()) {
                val card = cardRegistry.getCard(name) ?: continue
                CardArtAvatar.pathOf(card.metadata.imageUri)?.let(::add)
                CardArtAvatar.pathOf(card.backFace?.metadata?.imageUri)?.let(::add)
                for (printing in printingRegistry.printingsOf(name)) {
                    CardArtAvatar.pathOf(printing.imageUri)?.let(::add)
                    CardArtAvatar.pathOf(printing.backFaceImageUri)?.let(::add)
                }
            }
        }
    }

    fun isValid(value: String): Boolean =
        Avatars.isPreset(value) || CardArtAvatar.parse(value)?.let { it.path in knownArtPaths } == true
}
