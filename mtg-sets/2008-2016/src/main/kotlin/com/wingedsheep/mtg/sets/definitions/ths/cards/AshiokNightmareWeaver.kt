package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EntersWithCharacteristics
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Ashiok, Nightmare Weaver — Theros #188 (canonical printing)
 * {1}{U}{B}
 * Legendary Planeswalker — Ashiok
 * Loyalty 3
 * +2: Exile the top three cards of target opponent's library.
 * −X: Put a creature card with mana value X exiled with Ashiok onto the battlefield under your
 * control. That creature is a Nightmare in addition to its other types.
 * −10: Exile all cards from all opponents' hands and graveyards.
 *
 * Both exiling abilities file their cards in Ashiok's linked-exile pile (`linkToSource`, CR 607),
 * so the −X can return a creature exiled by either (first ruling). The −X reads the pile at
 * resolution — no target — and only a creature card whose mana value equals the chosen X is
 * eligible (`manaValueEqualsX()`, X from `loyaltyAbilityX`). The Nightmare type rides the entry
 * (`EntersWithCharacteristics`, as Ever After), lasting while it stays on the battlefield. A new
 * Ashiok is a new object with an empty pile (third ruling).
 */
val AshiokNightmareWeaver = card("Ashiok, Nightmare Weaver") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Planeswalker — Ashiok"
    startingLoyalty = 3
    oracleText = "+2: Exile the top three cards of target opponent's library.\n" +
        "−X: Put a creature card with mana value X exiled with Ashiok onto the battlefield under your " +
        "control. That creature is a Nightmare in addition to its other types.\n" +
        "−10: Exile all cards from all opponents' hands and graveyards."

    loyaltyAbility(+2) {
        target(Targets.Opponent)
        effect = Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(3, Player.TargetOpponent))
            exile(top, linkToSource = true)
        }
        description = "Exile the top three cards of target opponent's library."
    }

    loyaltyAbilityX {
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.FromLinkedExile())
            val chosen = chooseExactly(
                1,
                from = exiled,
                filter = GameObjectFilter.Creature.manaValueEqualsX(),
                prompt = "Choose a creature card with mana value X exiled with Ashiok to put onto the battlefield"
            )
            move(
                chosen,
                CardDestination.ToZone(Zone.BATTLEFIELD, Player.You),
                entersWith = EntersWithCharacteristics(addedSubtypes = setOf(Subtype.NIGHTMARE))
            )
        }
        description = "Put a creature card with mana value X exiled with Ashiok onto the battlefield " +
            "under your control. That creature is a Nightmare in addition to its other types."
    }

    loyaltyAbility(-10) {
        effect = Effects.Pipeline {
            val cards = gather(
                CardSource.FromMultipleZones(listOf(Zone.HAND, Zone.GRAVEYARD), Player.EachOpponent)
            )
            exile(cards, linkToSource = true)
        }
        description = "Exile all cards from all opponents' hands and graveyards."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "188"
        artist = "Karla Ortiz"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/599c3c9f-dfb7-4357-9d9c-9a1a4616b103.jpg?1783939731"
        ruling("2013-09-15", "Ashiok's second ability refers to any creature card exiled because of Ashiok's first or third ability.")
        ruling("2013-09-15", "Ashiok's second ability doesn't target any creature card. You choose which creature card to return when that ability resolves, but you must choose one with mana value equal to the value you chose for X when activating the ability.")
        ruling("2013-09-15", "If Ashiok leaves the battlefield, and later another Ashiok enters the battlefield, it is a new object (even if the two were represented by the same card). Creature cards exiled by the original Ashiok can't be put onto the battlefield by the second ability of the new Ashiok.")
        ruling("2013-09-15", "If you put a creature card with bestow onto the battlefield, it will be a creature on the battlefield, not an Aura.")
        ruling("2013-09-15", "If you put a creature card with {X} in its mana cost onto the battlefield, the value of that X is 0.")
    }
}
