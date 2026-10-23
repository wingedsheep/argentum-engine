package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.DynamicAmounts

/**
 * Jerren, Corrupted Bishop // Ormendahl, the Corrupter (Innistrad: Midnight Hunt #109)
 *
 * Front — Jerren, Corrupted Bishop ({2}{B}, Legendary Creature — Human Cleric, 2/3)
 *   Whenever Jerren enters or another nontoken Human you control dies, you lose 1 life and create
 *   a 1/1 white Human creature token.
 *   {2}: Target Human you control gains lifelink until end of turn.
 *   At the beginning of your end step, if you have exactly 13 life, you may pay {4}{B}{B}. If you
 *   do, transform Jerren.
 *
 * Back — Ormendahl, the Corrupter (Legendary Creature — Demon, 6/6, black color indicator)
 *   Flying, trample, lifelink
 *   Sacrifice another creature: Draw a card.
 *
 * "If you have exactly 13 life" is an intervening "if": it gates the trigger and is re-checked on
 * resolution, so a life total that moved off 13 in response means no payment is offered.
 */

private const val TOKEN_TEXT =
    "Whenever Jerren enters or another nontoken Human you control dies, you lose 1 life and create " +
        "a 1/1 white Human creature token."

private val loseLifeAndMakeHuman =
    Effects.LoseLife(1, EffectTarget.Controller) then Effects.CreateToken(
        power = 1,
        toughness = 1,
        colors = setOf(Color.WHITE),
        creatureTypes = setOf("Human"),
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7667345-e11b-4cad-ac4c-84eb1c5656c5.jpg?1783925225",
    )

private val JerrenFront = card("Jerren, Corrupted Bishop") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Cleric"
    power = 2
    toughness = 3
    oracleText = "$TOKEN_TEXT\n" +
        "{2}: Target Human you control gains lifelink until end of turn.\n" +
        "At the beginning of your end step, if you have exactly 13 life, you may pay {4}{B}{B}. " +
        "If you do, transform Jerren."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = loseLifeAndMakeHuman
        description = TOKEN_TEXT
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.withSubtype("Human").youControl().nontoken()).dies()
        effect = loseLifeAndMakeHuman
        description = TOKEN_TEXT
    }

    activatedAbility {
        cost = Costs.Mana("{2}")
        val human = target(TargetFilter(GameObjectFilter.Creature.withSubtype("Human").youControl()))
        effect = Effects.GrantKeyword(Keyword.LIFELINK, human)
        description = "{2}: Target Human you control gains lifelink until end of turn."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.CompareAmounts(DynamicAmounts.yourLifeTotal(), ComparisonOperator.EQ, 13)
        effect = Effects.MayPay(
            ManaCost.parse("{4}{B}{B}"),
            then = Effects.Transform(EffectTarget.Self),
        )
        description = "At the beginning of your end step, if you have exactly 13 life, you may pay " +
            "{4}{B}{B}. If you do, transform Jerren."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "109"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f6e668d-2502-4e82-b4c2-ef34c9afa27e.jpg?1783925620"
        ruling("2021-09-24", "Jerren's last ability will trigger only if you have exactly 13 life at the beginning of your end step. You will be able to pay {4}{B}{B} only if you have exactly 13 life as that ability begins to resolve. If your life total isn't 13 at that point, the ability will have no effect. You won't be able to pay {4}{B}{B} and Jerren won't transform.")
        ruling("2021-09-24", "If, while activating mana abilities during the resolution of Jerren's last ability, you somehow gain or lose life, you will still be able to finish paying the cost and Jerren will still transform.")
    }
}

private val OrmendahlTheCorrupter = card("Ormendahl, the Corrupter") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B"
    typeLine = "Legendary Creature — Demon"
    power = 6
    toughness = 6
    oracleText = "Flying, trample, lifelink\nSacrifice another creature: Draw a card."

    keywords(Keyword.FLYING, Keyword.TRAMPLE, Keyword.LIFELINK)

    activatedAbility {
        cost = Costs.SacrificeAnother(GameObjectFilter.Creature)
        effect = Effects.DrawCards(1)
        description = "Sacrifice another creature: Draw a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "109"
        artist = "Yongjae Choi"
        flavorText = "\"Ah, Jerren. You've been such a faithful, useful insect.\""
        imageUri = "https://cards.scryfall.io/normal/back/0/f/0f6e668d-2502-4e82-b4c2-ef34c9afa27e.jpg?1783925620"
    }
}

val JerrenCorruptedBishop: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = JerrenFront,
    backFace = OrmendahlTheCorrupter,
)
