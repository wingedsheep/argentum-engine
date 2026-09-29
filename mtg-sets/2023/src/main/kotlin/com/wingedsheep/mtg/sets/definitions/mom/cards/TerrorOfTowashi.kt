package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Terror of Towashi
 * {2}{B}{B}
 * Creature — Phyrexian Ogre
 * 4/3
 * Deathtouch
 * Whenever this creature attacks, you may pay {3}{B}. When you do, return target creature card
 * from your graveyard to the battlefield. It's a Phyrexian in addition to its other types.
 *
 * "You may pay {3}{B}. When you do, …" is a genuine reflexive trigger (CR 603.12): the target is
 * chosen only after the payment, when the reflexive ability goes on the stack. The Phyrexian type
 * is permanent — it has no duration on the card.
 */
val TerrorOfTowashi = card("Terror of Towashi") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Ogre"
    oracleText = "Deathtouch\n" +
        "Whenever this creature attacks, you may pay {3}{B}. When you do, return target creature " +
        "card from your graveyard to the battlefield. It's a Phyrexian in addition to its other types."
    power = 4
    toughness = 3

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayMana("{3}{B}"),
            descriptionOverride = "You may pay {3}{B}. When you do, return target creature card from " +
                "your graveyard to the battlefield. It's a Phyrexian in addition to its other types."
        ) {
            val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
            effect = Effects.PutOntoBattlefield(creatureCard) then
                Effects.AddCreatureType("Phyrexian", creatureCard, Duration.Permanent)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "331"
        artist = "Lius Lasahido"
        flavorText = "Takumi knew where all the bodies were buried. After all, he'd buried most of them."
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a759d44-5496-437e-a714-acc398988b84.jpg"
    }
}
