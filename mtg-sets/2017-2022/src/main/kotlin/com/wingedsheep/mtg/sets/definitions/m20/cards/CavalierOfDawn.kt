package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cavalier of Dawn — Core Set 2020 #10
 * {2}{W}{W}{W} · Creature — Elemental Knight 4/6
 *
 * Vigilance
 * When this creature enters, destroy up to one target nonland permanent. Its controller creates a
 * 3/3 colorless Golem artifact creature token.
 * When this creature dies, return target artifact or enchantment card from your graveyard to your
 * hand.
 *
 * With no target chosen, "its controller" names nobody and no Golem is created (2025-02-07
 * ruling); a legal target that survives (indestructible) still gives its controller a Golem.
 */
val CavalierOfDawn = card("Cavalier of Dawn") {
    manaCost = "{2}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elemental Knight"
    power = 4
    toughness = 6
    oracleText = "Vigilance\n" +
        "When this creature enters, destroy up to one target nonland permanent. Its controller creates a " +
        "3/3 colorless Golem artifact creature token.\n" +
        "When this creature dies, return target artifact or enchantment card from your graveyard to your hand."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter.NonlandPermanent, optional = true)
        effect = Effects.Destroy(permanent) then
            Effects.CreateToken(
                power = 3,
                toughness = 3,
                creatureTypes = setOf("Golem"),
                artifactToken = true,
                controller = EffectTarget.TargetController
            )
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val card = target(
            TargetFilter(GameObjectFilter.ArtifactOrEnchantment.ownedByYou(), zone = Zone.GRAVEYARD)
        )
        effect = Effects.ReturnToHandFromGraveyard(card)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "10"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7b1919e-c0c1-4ac7-9061-a337b6fe7273.jpg?1783933033"
        ruling(
            "2025-02-07",
            "You may put this creature's enters ability onto the stack without choosing any target. It will " +
                "resolve with no effect. However, if you do choose a target and the target permanent is an " +
                "illegal target by the time that ability tries to resolve, the ability won't resolve. No " +
                "player will create a Golem token. If the target is legal but not destroyed (most likely " +
                "because it has indestructible), its controller will create a Golem token."
        )
    }
}
