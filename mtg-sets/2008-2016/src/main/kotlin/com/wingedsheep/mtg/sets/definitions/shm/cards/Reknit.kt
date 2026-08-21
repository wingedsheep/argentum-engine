package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect

/**
 * Reknit
 * {1}{G/W}
 * Instant
 *
 * Regenerate target permanent.
 *
 * - The target is `Targets.Permanent`, not `Targets.Creature`: Reknit is one of the few
 *   regeneration spells that shields *any* permanent, so it can pre-empt a Disenchant or a
 *   land-destruction spell as well as combat damage.
 * - [RegenerateEffect] is target-type-agnostic — it creates a regeneration shield on whatever
 *   permanent it is pointed at — so no creature-specific vocabulary is needed.
 * - There is no `Effects.Regenerate` facade; the effect class is the shipped spelling
 *   (see Crypt Sliver).
 */
val Reknit = card("Reknit") {
    manaCost = "{1}{G/W}"
    typeLine = "Instant"
    oracleText = "Regenerate target permanent."

    spell {
        val t = target("target", Targets.Permanent)
        effect = RegenerateEffect(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "236"
        artist = "Zoltan Boros & Gabor Szikszai"
        flavorText = "\"An axe may break upon a ribbon if the ribbon's will is the stronger.\"\n" +
            "—Awylla, elvish safewright"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/10bde9be-0ad8-4f42-a9a9-838bc476c7a6.jpg?1783942716"
    }
}
