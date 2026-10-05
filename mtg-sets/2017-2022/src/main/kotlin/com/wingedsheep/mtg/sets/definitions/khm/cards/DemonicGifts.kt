package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Demonic Gifts — Kaldheim #84
 * {1}{B} · Instant · Common
 *
 * Until end of turn, target creature gets +2/+0 and gains "When this creature dies, return it to
 * the battlefield under its owner's control."
 *
 * The plain form of Presumed Dead / Fake Your Own Death: an end-of-turn +2/+0 plus an end-of-turn
 * granted self dies-trigger whose body moves the card graveyard → battlefield under its owner's
 * control (the `Move` default). The `fromZone = GRAVEYARD` gate makes it a no-op if the card already
 * left the graveyard in response. The returned creature is a new object, so it won't come back a
 * second time (see ruling).
 */
val DemonicGifts = card("Demonic Gifts") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature gets +2/+0 and gains \"When this creature dies, " +
        "return it to the battlefield under its owner's control.\""

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 0, creature, Duration.EndOfTurn) then
            Effects.GrantTriggeredAbility(
                ability = TriggeredAbility.create(
                    trigger = Triggers.self.dies(),
                    effect = Effects.Move(
                        target = EffectTarget.Self,
                        destination = Zone.BATTLEFIELD,
                        fromZone = Zone.GRAVEYARD
                    ),
                    descriptionOverride = "When this creature dies, return it to the battlefield " +
                        "under its owner's control."
                ),
                target = creature,
                duration = Duration.EndOfTurn
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Kekai Kotaki"
        flavorText = "It began as a flavor on the tongue, like a bloody morsel of roast boar—but soon " +
            "the power was a conflagration in her veins."
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f23c487-ac4a-475b-ad5e-ec6f8678e668.jpg?1783928252"

        ruling(
            "2021-02-05",
            "The reanimating effect of Demonic Gifts works only once. Once the creature dies and " +
                "returns to the battlefield, it's a new object with no relation to the creature it was. " +
                "If that new creature dies, it won't come back."
        )
    }
}
