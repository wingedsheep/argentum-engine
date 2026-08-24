package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CaptureControllersEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ForEachCapturedControllerEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Martyr's Cry
 * {W}{W}
 * Sorcery
 * Exile all white creatures. For each creature exiled this way, its controller draws a card.
 *
 * Builder's Bane's pipeline with exile in place of destroy. The order matters and is the reason this
 * isn't a simple two-step effect: `ControllerComponent` is stripped the moment a permanent leaves
 * the battlefield, so the controllers have to be **captured before the move** or the draws would
 * have nobody to belong to.
 *
 * `storeMovedAs` is what makes "exiled this way" literal: a white creature that never actually
 * reached exile — one with an exile-replacement, or one already gone — drops out of that collection
 * and so contributes no card to its controller.
 *
 * Every white creature on the battlefield, both sides; the sorcery is symmetrical, and its caster
 * draws for their own losses like everyone else.
 */
val MartyrsCry = card("Martyr's Cry") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Exile all white creatures. For each creature exiled this way, its controller draws a card."

    spell {
        effect = Effects.Composite(
            GatherCardsEffect(
                source = CardSource.FromZone(
                    zone = Zone.BATTLEFIELD,
                    player = Player.Each,
                    filter = GameObjectFilter.Creature.withColor(Color.WHITE),
                ),
                storeAs = "martyrs",
            ),
            CaptureControllersEffect(from = "martyrs", storeAs = "martyrControllers"),
            MoveCollectionEffect(
                from = "martyrs",
                destination = CardDestination.ToZone(Zone.EXILE),
                storeMovedAs = "exiledMartyrs",
            ),
            ForEachCapturedControllerEffect(
                collection = "exiledMartyrs",
                originalCollection = "martyrs",
                controllerSnapshot = "martyrControllers",
                countVariable = "martyrCount",
                effects = listOf(
                    Effects.DrawCards(DynamicAmount.VariableReference("martyrCount")),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "12"
        artist = "Jeff A. Menges"
        flavorText = "\"It is only fitting that one such as I should die in pursuit of knowledge.\"\n" +
            "—Vervamon the Elder"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2c9f463-d1cc-4f11-aad2-d4a4520aa978.jpg?1783947947"
    }
}
