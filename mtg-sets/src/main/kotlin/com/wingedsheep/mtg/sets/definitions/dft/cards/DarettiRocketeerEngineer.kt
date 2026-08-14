package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Daretti, Rocketeer Engineer — Aetherdrift #120.
 *
 * The graveyard card is targeted when either trigger goes on the stack. The artifact sacrifice is
 * an optional resolution-time cost, so declining it (or being unable to pay it) leaves the target
 * in the graveyard.
 */
val DarettiRocketeerEngineer = card("Daretti, Rocketeer Engineer") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Goblin Artificer"
    oracleText = "Daretti's power is equal to the greatest mana value among artifacts you control.\n" +
        "Whenever Daretti enters or attacks, choose target artifact card in your graveyard. You may " +
        "sacrifice an artifact. If you do, return the chosen card to the battlefield."
    toughness = 5

    dynamicPower(
        DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).maxManaValue()
    )

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val artifactCard = target(
            "target artifact card in your graveyard",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.Artifact.ownedByYou(),
                    zone = Zone.GRAVEYARD
                )
            )
        )
        effect = GatedEffect(
            gate = Gate.MayPay(SacrificeEffect(GameObjectFilter.Artifact)),
            then = Effects.Move(artifactCard, Zone.BATTLEFIELD)
        )
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        val artifactCard = target(
            "target artifact card in your graveyard",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.Artifact.ownedByYou(),
                    zone = Zone.GRAVEYARD
                )
            )
        )
        effect = GatedEffect(
            gate = Gate.MayPay(SacrificeEffect(GameObjectFilter.Artifact)),
            then = Effects.Move(artifactCard, Zone.BATTLEFIELD)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "120"
        artist = "Borja Pindado"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be626e2f-1075-4497-bd5b-bd805777afd3.jpg?1783907885"
        ruling("2025-02-07", "If an artifact on the battlefield has {X} in its mana cost, X is 0 when determining its mana value.")
        ruling("2025-02-07", "You choose the target card in your graveyard as Daretti's second ability is put on the stack, but you don't choose whether to sacrifice an artifact or which one to sacrifice until the ability resolves.")
    }
}
