package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Kiora, the Crashing Wave — Born of the Gods #149
 * {2}{G}{U} · Legendary Planeswalker — Kiora · Starting loyalty 2
 *
 * +1: Until your next turn, prevent all damage that would be dealt to and dealt by target
 *     permanent an opponent controls.
 * −1: Draw a card. You may play an additional land this turn.
 * −5: You get an emblem with "At the beginning of your end step, create a 9/9 blue Kraken
 *     creature token."
 *
 * The +1 is Dovin, Hand of Control's −1: two all-damage shields bound to the target, one on
 * damage dealt to it and one on damage dealt by it, both until your next turn. The shields
 * follow the object, so they keep applying if it changes controllers (2014-02-01 ruling).
 * The −5 is Ajani, Adversary of Tyrants' end-step token emblem.
 */
val KioraTheCrashingWave = card("Kiora, the Crashing Wave") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Planeswalker — Kiora"
    startingLoyalty = 2
    oracleText = "+1: Until your next turn, prevent all damage that would be dealt to and dealt by " +
        "target permanent an opponent controls.\n" +
        "−1: Draw a card. You may play an additional land this turn.\n" +
        "−5: You get an emblem with \"At the beginning of your end step, create a 9/9 blue Kraken " +
        "creature token.\""

    loyaltyAbility(+1) {
        val permanent = target(TargetFilter.PermanentOpponentControls)
        effect = Effects.PreventDamage(target = permanent, duration = Duration.UntilYourNextTurn) then
            Effects.PreventAllDamageDealtBy(permanent, duration = Duration.UntilYourNextTurn)
        description = "Until your next turn, prevent all damage that would be dealt to and dealt by " +
            "target permanent an opponent controls."
    }

    loyaltyAbility(-1) {
        effect = Effects.DrawCards(1) then Effects.PlayAdditionalLands(1)
        description = "Draw a card. You may play an additional land this turn."
    }

    loyaltyAbility(-5) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.you.beginningOf(Step.END)
                effect = Effects.CreateToken(
                    power = 9,
                    toughness = 9,
                    colors = setOf(Color.BLUE),
                    creatureTypes = setOf("Kraken"),
                    imageUri = "https://cards.scryfall.io/normal/front/f/9/f9576efe-03eb-49da-bb00-5bf8e50877fb.jpg?1783939516"
                )
                description = "At the beginning of your end step, create a 9/9 blue Kraken creature token."
            },
            emblem = true,
            descriptionOverride = "At the beginning of your end step, create a 9/9 blue Kraken creature token."
        )
        description = "You get an emblem with \"At the beginning of your end step, create a 9/9 blue " +
            "Kraken creature token.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "149"
        artist = "Scott M. Fischer"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f77ac7fa-718d-425f-a0aa-2f8e2bfc0950.jpg?1783939525"

        ruling("2014-02-01", "Kiora's first ability can target any permanent an opponent controls, not just one that can deal or be dealt damage.")
        ruling("2014-02-01", "Once the first ability resolves, the damage prevention continues to apply even if that permanent changes controllers.")
        ruling("2014-02-01", "Kiora's second ability allows you to play an additional land during your main phase. Doing so follows the normal timing rules for playing lands. In particular, you won't play a land as that ability resolves. The ability will fully resolve (and you'll draw a card, perhaps a land you'll play later) first.")
        ruling("2014-02-01", "The effect of the second ability is cumulative with other effects that let you play additional lands, such as the one from Rites of Flourishing.")
        ruling("2014-02-01", "If Kiora leaves the battlefield after her second ability is activated but before it resolves, you'll still be able to play an additional land after the ability resolves.")
    }
}
