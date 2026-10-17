package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Geothermal Kami — Kamigawa: Neon Dynasty #186 (canonical printing)
 * {3}{G} · Creature — Spirit · 4/3
 *
 * When this creature enters, you may return an enchantment you control to its owner's hand.
 * If you do, you gain 3 life.
 *
 * The enchantment is chosen on resolution (no target). [Effects.IfYouDo] gates the life gain on
 * the bounce actually moving a card, so with no enchantment to return nothing happens.
 */
val GeothermalKami = card("Geothermal Kami") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spirit"
    power = 4
    toughness = 3
    oracleText = "When this creature enters, you may return an enchantment you control to its owner's hand. If you do, you gain 3 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.Pipeline {
                    val enchantments = gather(
                        CardSource.BattlefieldMatching(filter = GameObjectFilter.Enchantment, player = Player.You)
                    )
                    val returned = chooseExactly(
                        1,
                        from = enchantments,
                        prompt = "Return a permanent you control to its owner's hand",
                        useTargetingUI = true
                    )
                    toHand(returned)
                },
                then = Effects.GainLife(3)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "186"
        artist = "Rudy Siswanto"
        flavorText = "A mysterious kami will sometimes appear to overworked monks and offer them a warm, relaxing bath."
        imageUri = "https://cards.scryfall.io/normal/front/5/5/559d7162-39fc-4d99-8c5f-b40a765b883a.jpg?1783923849"
    }
}
