package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Phytotitan (Magic 2015 #191)
 * {4}{G}{G} · Creature — Plant Elemental · 7/2
 *
 * When this creature dies, return it to the battlefield tapped under its owner's control at the
 * beginning of their next upkeep.
 *
 * The dies trigger schedules a one-shot step trigger at upkeep gated to the card's *owner's* turn
 * (`fireOnPlayer = OwnerOfSource`), so a Phytotitan that died while stolen still waits for its
 * owner's upkeep rather than the thief's. The move names `Self` with `fromZone = GRAVEYARD`, so a
 * card that has left the graveyard before the trigger resolves stays where it is (ruling). A card
 * put onto the battlefield from the graveyard enters under its owner's control unless told
 * otherwise, which is exactly "under its owner's control".
 */
val Phytotitan = card("Phytotitan") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant Elemental"
    power = 7
    toughness = 2
    oracleText = "When this creature dies, return it to the battlefield tapped under its owner's control " +
        "at the beginning of their next upkeep."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateDelayedTrigger(
            step = Step.UPKEEP,
            fireOnPlayer = EffectTarget.PlayerRef(Player.OwnerOfSource),
            effect = Effects.Move(
                EffectTarget.Self,
                Zone.BATTLEFIELD,
                ZonePlacement.Tapped,
                fromZone = Zone.GRAVEYARD,
            ),
        )
        description = "When this creature dies, return it to the battlefield tapped under its owner's " +
            "control at the beginning of their next upkeep."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "191"
        artist = "Marco Nelor"
        flavorText = "Its root system spans the entire floor of the jungle, making eradication impossible."
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0e6eedf-8f5a-407e-b15b-48e19df20bed.jpg?1783939163"
        ruling(
            "2014-07-18",
            "Phytotitan's ability will return it to the battlefield only if it's still in the graveyard " +
                "when the delayed triggered ability resolves. If it's not, it won't return to the battlefield."
        )
    }
}
