package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Captain Benjamin Sisko
 * {1}{W}
 * Legendary Creature — Human Officer
 * 2/2
 *
 * At the beginning of your end step, create a 1/1 red Officer creature token for each dilemma
 * you've faced this turn.
 * {2}{W}{U}{B}{R}{G}, {T}: Exile the top five cards of your library. You may cast any number of
 * spells with mana value less than or equal to the number of Officers you control from among those
 * cards without paying their mana costs.
 *
 * The end-step count reads the per-turn dilemma tracker (one per mode choice you made this turn).
 * The activated ability mirrors Villainous Wealth: the Officer count is read as the ability
 * resolves, lands are never castable, and uncast cards stay in exile.
 */
val CaptainBenjaminSisko = card("Captain Benjamin Sisko") {
    manaCost = "{1}{W}"
    typeLine = "Legendary Creature — Human Officer"
    colorIdentity = "WUBRG"
    oracleText = "At the beginning of your end step, create a 1/1 red Officer creature token for each " +
        "dilemma you've faced this turn.\n{2}{W}{U}{B}{R}{G}, {T}: Exile the top five cards of your " +
        "library. You may cast any number of spells with mana value less than or equal to the number of " +
        "Officers you control from among those cards without paying their mana costs."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.CreateToken(
            count = DynamicAmounts.dilemmasFacedThisTurn(),
            power = 1, toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Officer"),
            imageUri = "https://cards.scryfall.io/normal/front/d/6/d676cbef-8e31-4ae6-8dfe-6b84160f71e0.jpg?1793363893"
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{W}{U}{B}{R}{G}"), Costs.Tap)
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(5))
            exile(exiled)
            val spells = filter(exiled, GameObjectFilter.Nonland)
            val officers = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype("Officer")).count()
            val castable = filter(spells, GameObjectFilter.Any.manaValueAtMostDynamic(officers))
            run(Effects.CastAnyNumberFromCollectionWithoutPayingCost(castable))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "6"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51338fb2-13a3-41b8-981f-05fa35e87e2e.jpg?1792508139"
    }
}
