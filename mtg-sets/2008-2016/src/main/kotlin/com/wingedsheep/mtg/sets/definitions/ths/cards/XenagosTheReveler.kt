package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Xenagos, the Reveler - {2}{R}{G}
 * Legendary Planeswalker — Xenagos
 * Starting Loyalty: 3
 *
 * +1: Add X mana in any combination of {R} and/or {G}, where X is the number of creatures you control.
 * 0: Create a 2/2 red and green Satyr creature token with haste.
 * −6: Exile the top seven cards of your library. You may put any number of creature and/or land
 * cards from among them onto the battlefield.
 *
 * The +1 is a loyalty ability, not a mana ability (the mana-ability rule excludes loyalty abilities), so it
 * uses the stack; X is counted on resolution and the {R}/{G} split is chosen then
 * ([Effects.AddDynamicMana] with two colors — the Grand Warlord Radha shape).
 * The −6 is the Tezzeret, Master of the Bridge exile-then-pick shape, with an any-number choice.
 */
val XenagosTheReveler = card("Xenagos, the Reveler") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Planeswalker — Xenagos"
    startingLoyalty = 3
    oracleText = "+1: Add X mana in any combination of {R} and/or {G}, where X is the number of creatures you control.\n" +
        "0: Create a 2/2 red and green Satyr creature token with haste.\n" +
        "−6: Exile the top seven cards of your library. You may put any number of creature and/or land " +
        "cards from among them onto the battlefield."

    // +1: Add X mana in any combination of {R} and/or {G}.
    loyaltyAbility(+1) {
        effect = Effects.AddDynamicMana(
            amount = DynamicAmounts.creaturesYouControl(),
            allowedColors = setOf(Color.RED, Color.GREEN)
        )
        description = "Add X mana in any combination of {R} and/or {G}, where X is the number of creatures you control."
    }

    // 0: Create a 2/2 red and green Satyr creature token with haste.
    loyaltyAbility(0) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.RED, Color.GREEN),
            creatureTypes = setOf("Satyr"),
            keywords = setOf(Keyword.HASTE)
        )
        description = "Create a 2/2 red and green Satyr creature token with haste."
    }

    // −6: Exile the top seven; put any number of creature and/or land cards onto the battlefield.
    loyaltyAbility(-6) {
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(7))
            exile(exiled)
            val chosen = chooseAnyNumber(
                from = exiled,
                filter = GameObjectFilter.Creature or GameObjectFilter.Land,
                prompt = "Put any number of creature and/or land cards onto the battlefield",
                selectedLabel = "Put onto the battlefield",
                remainderLabel = "Leave in exile"
            )
            move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
        }
        description = "Exile the top seven cards of your library. You may put any number of creature " +
            "and/or land cards from among them onto the battlefield."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "209"
        artist = "Jason Chan"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3aecfe99-9c4b-4087-84ee-fb208fd22820.jpg?1783939722"
    }
}
