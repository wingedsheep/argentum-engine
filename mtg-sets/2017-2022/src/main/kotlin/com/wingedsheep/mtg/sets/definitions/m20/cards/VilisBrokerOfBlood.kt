package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vilis, Broker of Blood — Core Set 2020 #122 (canonical printing)
 * {5}{B}{B}{B} · Legendary Creature — Demon 8/8
 *
 * Flying
 * {B}, Pay 2 life: Target creature gets -1/-1 until end of turn.
 * Whenever you lose life, draw that many cards.
 *
 * The draw reads the triggering loss (Lich's Mastery's `triggerLifeLost`); paying 2 life for the
 * activated ability is itself a loss, so it triggers too, after the activation completes (ruling).
 */
val VilisBrokerOfBlood = card("Vilis, Broker of Blood") {
    manaCost = "{5}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Demon"
    power = 8
    toughness = 8
    oracleText = "Flying\n" +
        "{B}, Pay 2 life: Target creature gets -1/-1 until end of turn.\n" +
        "Whenever you lose life, draw that many cards. (Damage causes loss of life.)"

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.PayLife(2))
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-1, -1, t)
        description = "{B}, Pay 2 life: Target creature gets -1/-1 until end of turn."
    }

    triggeredAbility {
        trigger = Triggers.you.losesLife()
        effect = Effects.DrawCards(DynamicAmounts.triggerLifeLost())
        description = "Whenever you lose life, draw that many cards."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "122"
        artist = "Tyler Jacobson"
        flavorText = "\"You can sign, or you can spend your life imagining what might have been yours.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ecdf2bd9-87b9-470a-ad2e-0ebf98560f87.jpg?1783932985"
        ruling("2019-07-12", "A player loses life if they pay life.")
        ruling(
            "2019-07-12",
            "If you pay life to cast a spell or activate an ability, you don't draw cards until after you've " +
                "finished casting that spell or activating that ability. You draw those cards before that spell " +
                "or ability resolves, but after targets are chosen for it."
        )
    }
}
