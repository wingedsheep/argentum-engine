package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Emet-Selch, Unsundered // Hades, Sorcerer of Eld — Final Fantasy #218
 * {1}{U}{B} · Legendary Creature — Elder Wizard 2/4
 * // Legendary Creature — Avatar 6/6
 *
 * Front — Emet-Selch, Unsundered:
 *   Vigilance
 *   Whenever Emet-Selch enters or attacks, draw a card, then discard a card.
 *   At the beginning of your upkeep, if there are fourteen or more cards in your graveyard,
 *   you may transform Emet-Selch.
 *
 * Back — Hades, Sorcerer of Eld:
 *   Vigilance
 *   Echo of the Lost — During your turn, you may play cards from your graveyard.
 *   If a card or token would be put into your graveyard from anywhere, exile it instead.
 *
 * "Enters or attacks" is two distinct trigger conditions, so the loot is modelled as two
 * triggered abilities (`EntersBattlefield` + self `Attacks`), each running the standard
 * draw-then-discard [Patterns.Hand.loot]. The upkeep transform is an intervening-"if" trigger
 * ([triggerCondition]) — per the ruling, the fourteen-card graveyard check is evaluated both when
 * the ability would trigger and again as it resolves — wrapped in [MayEffect] for the optional
 * "you may transform" and flipping the permanent in place with [TransformEffect].
 *
 * Hades' back face reuses the Yawgmoth's Agenda shape: [MayPlayLandsFromGraveyard] (land-play
 * timing already enforces "during your turn") + [MayCastFromGraveyard] gated to your turn covers
 * "play cards from your graveyard", and a [RedirectZoneChange] to exile catches every card or
 * token you own that would hit your graveyard from anywhere.
 */
private val HadesSorcererOfEld = card("Hades, Sorcerer of Eld") {
    manaCost = ""
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Avatar"
    oracleText = "Vigilance\n" +
        "Echo of the Lost — During your turn, you may play cards from your graveyard.\n" +
        "If a card or token would be put into your graveyard from anywhere, exile it instead."
    power = 6
    toughness = 6
    keywords(Keyword.VIGILANCE)

    // Echo of the Lost — During your turn, you may play cards from your graveyard.
    // Lands: normal land-play timing already restricts this to your own turn.
    staticAbility {
        ability = MayPlayLandsFromGraveyard
    }
    // Spells: gated to your turn so instants from the graveyard can't be cast on opponents' turns.
    staticAbility {
        ability = MayCastFromGraveyard(filter = GameObjectFilter.Nonland, duringYourTurnOnly = true)
    }

    // If a card or token would be put into your graveyard from anywhere, exile it instead.
    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter(
                    controllerPredicate = ControllerPredicate.OwnedByYou
                ),
                to = Zone.GRAVEYARD
            )
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "218"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/back/7/5/75cf4eb8-33e7-4dfc-b890-a7e3b5c1b9d5.jpg?1782686432"
        ruling(
            "2025-06-06",
            "While you control Hades, Sorcerer of Eld, abilities that trigger whenever a permanent " +
                "you own is put into your graveyard from the battlefield (for example, \"When this " +
                "creature dies . . .\") won't trigger."
        )
        ruling(
            "2025-06-06",
            "If you discard a card while you control Hades, Sorcerer of Eld, abilities that function " +
                "when a card is discarded (such as madness) still work, even though that card never " +
                "reaches your graveyard. Spells or abilities that check the characteristics of a " +
                "discarded card can find that card in exile."
        )
        ruling(
            "2025-06-06",
            "You pay all costs and follow all timing rules for cards played with the permission " +
                "granted by Hades, Sorcerer of Eld's second ability."
        )
    }
}

private val EmetSelchUnsunderedFront = card("Emet-Selch, Unsundered") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Elder Wizard"
    oracleText = "Vigilance\n" +
        "Whenever Emet-Selch enters or attacks, draw a card, then discard a card.\n" +
        "At the beginning of your upkeep, if there are fourteen or more cards in your graveyard, " +
        "you may transform Emet-Selch."
    power = 2
    toughness = 4
    keywords(Keyword.VIGILANCE)

    // Whenever Emet-Selch enters or attacks, draw a card, then discard a card.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Hand.loot()
    }
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Patterns.Hand.loot()
    }

    // At the beginning of your upkeep, if there are fourteen or more cards in your graveyard,
    // you may transform Emet-Selch. Intervening "if" — checked at trigger time and again on
    // resolution.
    triggeredAbility {
        trigger = Triggers.YourUpkeep
        triggerCondition = Conditions.CardsInGraveyardAtLeast(14)
        effect = MayEffect(TransformEffect(EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "218"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75cf4eb8-33e7-4dfc-b890-a7e3b5c1b9d5.jpg?1782686432"
        ruling(
            "2025-06-06",
            "Emet-Selch's last ability checks your graveyard at the moment it would trigger to see " +
                "if you have fourteen or more cards in your graveyard. If you don't, the ability " +
                "won't trigger at all. If it does trigger, the ability will check again as it tries " +
                "to resolve. If you don't have fourteen or more cards in your graveyard at that " +
                "time, the ability won't resolve and none of its effects will happen."
        )
        ruling(
            "2025-06-06",
            "The mana value of a nonmodal double-faced card is the mana value of its front face, " +
                "no matter which face is up."
        )
    }
}

val EmetSelchUnsundered: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = EmetSelchUnsunderedFront,
    backFace = HadesSorcererOfEld,
)
