package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AddCountersToCollectionEffect
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Sourbread Auntie
 * {2}{R}{R}
 * Creature — Goblin Warrior
 * 4/3
 *
 * When this creature enters, you may blight 2. If you do, create two 1/1 black
 * and red Goblin creature tokens.
 * (To blight 2, put two -1/-1 counters on a creature you control.)
 */
val SourbreadAuntie = card("Sourbread Auntie") {
    manaCost = "{2}{R}{R}"
    typeLine = "Creature — Goblin Warrior"
    power = 4
    toughness = 3
    oracleText = "When this creature enters, you may blight 2. If you do, create two 1/1 black and red Goblin creature tokens. " +
        "(To blight 2, put two -1/-1 counters on a creature you control.)"

    val createGoblinTokens = Effects.CreateToken(
        power = 1,
        toughness = 1,
        colors = setOf(Color.BLACK, Color.RED),
        creatureTypes = setOf("Goblin"),
        count = 2,
        imageUri = "https://cards.scryfall.io/normal/front/6/1/6139a45d-ebc7-4bca-8c13-73c85ea5fe0d.jpg?1768367480"
    )

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            effect = CompositeEffect(
                listOf(
                    GatherCardsEffect(
                        source = CardSource.ControlledPermanents(Player.You, GameObjectFilter.Creature),
                        storeAs = "blightTargets"
                    ),
                    SelectFromCollectionEffect(
                        from = "blightTargets",
                        selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                        chooser = Chooser.Controller,
                        storeSelected = "blighted",
                        prompt = "Blight 2 — choose a creature you control (or cancel)",
                        useTargetingUI = true,
                        alwaysPrompt = true
                    ),
                    AddCountersToCollectionEffect("blighted", Counters.MINUS_ONE_MINUS_ONE, 2),
                    ConditionalOnCollectionEffect(
                        collection = "blighted",
                        ifNotEmpty = createGoblinTokens
                    )
                )
            ),
            description_override = "You may blight 2. If you do, create two 1/1 black and red Goblin creature tokens."
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "158"
        artist = "John Tedrick"
        flavorText = "\"C'mere you little urchins. Auntie's trying a new recipe, and I need some tasters.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32fd226f-d1c6-432c-b846-9482f1944363.jpg?1767732766"
    }
}
