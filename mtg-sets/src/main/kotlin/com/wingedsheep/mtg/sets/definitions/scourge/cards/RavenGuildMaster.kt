package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

val RavenGuildMaster = card("Raven Guild Master") {
    manaCost = "{1}{U}{U}"
    typeLine = "Creature — Human Wizard Mutant"
    power = 1
    toughness = 1
    oracleText = "Whenever Raven Guild Master deals combat damage to a player, that player exiles the top ten cards of their library.\nMorph {2}{U}{U}"

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(10), Player.TriggeringPlayer),
                    storeAs = "exiled"
                ),
                MoveCollectionEffect(
                    from = "exiled",
                    destination = CardDestination.ToZone(Zone.EXILE, Player.TriggeringPlayer)
                )
            )
        )
    }

    morph = "{2}{U}{U}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "47"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/9843f847-6a8f-4042-86b6-f7fe5a47cc57.jpg?1562532562"
    }
}
