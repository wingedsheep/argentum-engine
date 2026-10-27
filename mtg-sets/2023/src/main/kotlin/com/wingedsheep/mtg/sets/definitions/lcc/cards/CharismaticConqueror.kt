package com.wingedsheep.mtg.sets.definitions.lcc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Charismatic Conqueror
 * {1}{W}
 * Creature — Vampire Soldier
 * 2/2
 * Vigilance
 * Whenever an artifact or creature an opponent controls enters untapped, they may tap that
 * permanent. If they don't, you create a 1/1 white Vampire creature token with lifelink.
 *
 * "Enters untapped" is the `untapped()` state predicate on the enters trigger, which the trigger
 * matcher reads off how the permanent *entered* (not its tap state at detection). The choice is
 * the permanent's controller's ([EffectTarget.ControllerOfTriggeringEntity]). Per the 2023-11-10
 * ruling, a permanent that is already tapped or gone when the ability resolves can't be tapped,
 * so the token is created without asking — the outer `If` checks the permanent is still an
 * untapped permanent on the battlefield before offering the choice.
 */
val CharismaticConqueror = card("Charismatic Conqueror") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Vampire Soldier"
    power = 2
    toughness = 2
    oracleText = "Vigilance\nWhenever an artifact or creature an opponent controls enters untapped, they may " +
        "tap that permanent. If they don't, you create a 1/1 white Vampire creature token with lifelink."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.CreatureOrArtifact.opponentControls().untapped()).enters()
        val vampire = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Vampire"),
            keywords = setOf(Keyword.LIFELINK),
            imageUri = "https://cards.scryfall.io/normal/front/0/4/0484390e-1167-4407-84de-7ddc726e8926.jpg?1783913608"
        )
        effect = Effects.If(
            Conditions.EntityMatches(EffectTarget.TriggeringEntity, GameObjectFilter.Any.untapped().onBattlefield()),
            then = Effects.May(
                Effects.Tap(EffectTarget.TriggeringEntity),
                otherwise = vampire,
                decisionMaker = EffectTarget.ControllerOfTriggeringEntity,
                prompt = "Tap that permanent? If you don't, your opponent creates a 1/1 Vampire with lifelink."
            ),
            otherwise = vampire
        )
        description = "Whenever an artifact or creature an opponent controls enters untapped, they may tap " +
            "that permanent. If they don't, you create a 1/1 white Vampire creature token with lifelink."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "70"
        artist = "Bram Sels"
        flavorText = "\"There's a whole world under the surface, waiting for the light of our salvation.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/9/599c934d-bfff-43ce-a545-6e3cde124515.jpg?1783913915"
        ruling(
            "2023-11-10",
            "If the permanent is tapped as the ability resolves, the opponent can't choose to tap it, so " +
                "you'll create a Vampire token. Similarly, if it's no longer on the battlefield as the ability " +
                "resolves, they can't choose to tap it, and you'll create a Vampire token."
        )
    }
}
