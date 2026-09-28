package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Invasion of Segovia // Caetus, Sea Tyrant of Segovia — March of the Machine #63.
 * {2}{U} · Battle — Siege · defense 4 // Legendary Creature — Serpent 3/3
 *
 * When this Siege enters, create two 1/1 blue Kraken creature tokens with trample.
 * // Noncreature spells you cast have convoke.
 * // At the beginning of your end step, untap up to four target creatures.
 *
 * Caetus grants convoke at runtime to noncreature spells only ([GrantKeywordToOwnSpells], the
 * same grant Eirdu uses for creature spells). The end-step untap is any creatures, not just yours.
 */
private val InvasionOfSegoviaFront = card("Invasion of Segovia") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Battle — Siege"
    startingDefense = 4
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, create two 1/1 blue Kraken creature tokens with trample."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Kraken"),
            keywords = setOf(Keyword.TRAMPLE),
            imageUri = "https://cards.scryfall.io/normal/front/c/b/cb727dec-dd82-4072-b2ec-a4e31b58752f.jpg?1783916673",
        )
        description = "When this Siege enters, create two 1/1 blue Kraken creature tokens with trample."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Edgar Sánchez Hidalgo"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9df3e743-7bb8-482a-afd1-4d51119d416c.jpg?1783917037"
    }
}

private val CaetusSeaTyrantOfSegovia = card("Caetus, Sea Tyrant of Segovia") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U"
    typeLine = "Legendary Creature — Serpent"
    oracleText = "Noncreature spells you cast have convoke. (Your creatures can help cast those " +
        "spells. Each creature you tap while casting a noncreature spell pays for {1} or one mana " +
        "of that creature's color.)\n" +
        "At the beginning of your end step, untap up to four target creatures."
    power = 3
    toughness = 3

    staticAbility {
        ability = GrantKeywordToOwnSpells(
            keyword = Keyword.CONVOKE,
            spellFilter = GameObjectFilter.Noncreature,
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        targets(TargetFilter.Creature, count = 4, optional = true)
        effect = Effects.UntapEachTarget()
        description = "At the beginning of your end step, untap up to four target creatures."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Edgar Sánchez Hidalgo"
        imageUri = "https://cards.scryfall.io/normal/back/9/d/9df3e743-7bb8-482a-afd1-4d51119d416c.jpg?1783917037"
        ruling("2023-04-14", "If you sacrifice Caetus while casting a noncreature spell (say, to activate a mana ability), the spell won't have convoke when you pay its costs unless it has convoke some other way.")
    }
}

val InvasionOfSegovia: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfSegoviaFront,
    backFace = CaetusSeaTyrantOfSegovia,
)
