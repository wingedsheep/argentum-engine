package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Invasion of Belenon // Belenon War Anthem — March of the Machine #20 (canonical printing).
 * {2}{W} · Battle — Siege · defense 5 // Enchantment
 *
 * When this Siege enters, create a 2/2 white and blue Knight creature token with vigilance.
 * // Creatures you control get +1/+1.
 *
 * The Siege reminder text restates rules every battle has (protector choice, attack legality, the
 * intrinsic defeat trigger), so only `startingDefense` and the back face are declared here — see
 * Invasion of Innistrad.
 */
private val InvasionOfBelenonFront = card("Invasion of Belenon") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Battle — Siege"
    startingDefense = 5
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, create a 2/2 white and blue Knight creature token with vigilance."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE, Color.BLUE),
            creatureTypes = setOf("Knight"),
            keywords = setOf(Keyword.VIGILANCE),
            imageUri = "https://cards.scryfall.io/normal/front/8/8/88439bfc-8942-473b-9e4f-863017788476.jpg?1783916669"
        )
        description = "When this Siege enters, create a 2/2 white and blue Knight creature token with vigilance."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "20"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4fb0eb2d-9cb7-4e72-a970-5009b046df2a.jpg?1783917065"
    }
}

/**
 * The back face. Cast transformed, for free, by the Siege's defeat trigger — no mana cost, so a
 * white colour indicator.
 */
private val BelenonWarAnthem = card("Belenon War Anthem") {
    manaCost = ""
    colorIdentity = "W"
    colorIndicator = "W"
    typeLine = "Enchantment"
    oracleText = "Creatures you control get +1/+1."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "20"
        artist = "Antonio José Manzanedo"
        flavorText = "\"Just as a flute turns breath into music, we are the vessel through which " +
            "the Sacred Winds produce glorious justice.\""
        imageUri = "https://cards.scryfall.io/normal/back/4/f/4fb0eb2d-9cb7-4e72-a970-5009b046df2a.jpg?1783917065"
    }
}

val InvasionOfBelenon: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfBelenonFront,
    backFace = BelenonWarAnthem,
)
