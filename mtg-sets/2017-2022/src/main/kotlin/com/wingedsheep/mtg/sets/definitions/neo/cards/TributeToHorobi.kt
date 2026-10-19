package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private const val RAT_ROGUE_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/6/9/695f72ea-13e6-40ab-afc7-55657701c82d.jpg?1783923715"

/**
 * Tribute to Horobi // Echo of Death's Wail
 * {1}{B} — Enchantment — Saga // Enchantment Creature — Spirit 3/3
 *
 * Front — Tribute to Horobi:
 *   I, II — Each opponent creates a 1/1 black Rat Rogue creature token.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Echo of Death's Wail:
 *   Flying, haste
 *   When this creature enters, gain control of all Rat tokens.
 *   Whenever this creature attacks, you may sacrifice another creature. If you do, draw a card.
 *
 * "All Rat tokens" is every Rat token on the battlefield, whoever controls it — the opponents'
 * chapter I/II Rats, but also any other Rat tokens. The control change has no duration.
 */
private val EchoOfDeathsWail = card("Echo of Death's Wail") {
    manaCost = ""
    colorIndicator = "B"
    colorIdentity = "B"
    typeLine = "Enchantment Creature — Spirit"
    oracleText = "Flying, haste\n" +
        "When this creature enters, gain control of all Rat tokens.\n" +
        "Whenever this creature attacks, you may sacrifice another creature. If you do, draw a card."
    power = 3
    toughness = 3

    keywords(Keyword.FLYING, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Permanent.withSubtype("Rat").token()),
            Effects.GainControl(EffectTarget.IterationEntity, Duration.Permanent),
        )
        description = "When this creature enters, gain control of all Rat tokens."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            Effects.SacrificeOwn(GameObjectFilter.Creature, excludeSource = true),
            then = Effects.DrawCards(1),
        )
        description = "Whenever this creature attacks, you may sacrifice another creature. If you do, draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Robin Olausson"
        imageUri = "https://cards.scryfall.io/normal/back/5/4/54613f38-ca91-4764-b1fe-60cd83707fb4.jpg?1783923882"
    }
}

private val TributeToHorobiFront = card("Tribute to Horobi") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I, II — Each opponent creates a 1/1 black Rat Rogue creature token.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    for (chapter in 1..2) {
        sagaChapter(chapter) {
            effect = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Rat", "Rogue"),
                controller = EffectTarget.PlayerRef(Player.EachOpponent),
                imageUri = RAT_ROGUE_TOKEN_IMAGE,
            )
        }
    }

    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Robin Olausson"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54613f38-ca91-4764-b1fe-60cd83707fb4.jpg?1783923882"
    }
}

val TributeToHorobi: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = TributeToHorobiFront,
    backFace = EchoOfDeathsWail,
)
