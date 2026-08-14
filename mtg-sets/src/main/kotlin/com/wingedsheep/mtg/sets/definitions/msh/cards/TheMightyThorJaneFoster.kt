package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * The Mighty Thor, Jane Foster
 * {1}{W}{U}
 * Legendary Creature — Human God Hero
 * 3/3
 *
 * Flying
 * Whenever The Mighty Thor attacks, exile up to one target nontoken artifact or creature, then
 *   return that card to the battlefield tapped under its owner's control.
 * Whenever an Equipment you control enters, draw a card.
 *
 *  - **The attack trigger is a blink, not removal.** "Up to one target" is
 *    [TargetPermanent]`(optional = true)`, so Thor may attack with the trigger on the stack and no
 *    target chosen (and the trigger still resolves harmlessly if the only legal target is gone by
 *    resolution). The body is Splash Portal's pair — [Effects.Move] to [Zone.EXILE] then straight
 *    back to [Zone.BATTLEFIELD] — with [ZonePlacement.Tapped] for "tapped". The return names no
 *    controller override because a permanent put onto the battlefield by an effect that doesn't
 *    say otherwise enters under its *owner's* control, which is exactly the printed wording; so
 *    blinking an opponent's creature hands it back to them, tapped and freshly summoning-sick,
 *    rather than stealing it.
 *  - **Nontoken** is load-bearing: a token that leaves the battlefield ceases to exist (CR 111.7)
 *    and could never come back, so the printed card excludes tokens from the target set rather
 *    than offering a strictly-better removal mode.
 *  - The Equipment payoff is Giott, King of the Dwarves' shape: [Triggers.entersBattlefield] with
 *    an "Equipment you control" filter and [TriggerBinding.ANY] (Thor is not an Equipment, so the
 *    binding is purely about watching other permanents). It fires for *every* Equipment, tokens
 *    included, and has no once-per-turn cap.
 */
val TheMightyThorJaneFoster = card("The Mighty Thor, Jane Foster") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human God Hero"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever The Mighty Thor attacks, exile up to one target nontoken artifact or creature, " +
        "then return that card to the battlefield tapped under its owner's control.\n" +
        "Whenever an Equipment you control enters, draw a card."

    keywords(Keyword.FLYING)

    // Whenever The Mighty Thor attacks, exile up to one target nontoken artifact or creature,
    // then return that card to the battlefield tapped under its owner's control.
    triggeredAbility {
        trigger = Triggers.Attacks
        val blinked = target(
            "nontoken artifact or creature",
            TargetPermanent(
                optional = true,
                filter = TargetFilter(GameObjectFilter.CreatureOrArtifact.nontoken()),
            ),
        )
        effect = Effects.Move(blinked, Zone.EXILE)
            .then(Effects.Move(blinked, Zone.BATTLEFIELD, placement = ZonePlacement.Tapped))
        description = "Whenever The Mighty Thor attacks, exile up to one target nontoken artifact " +
            "or creature, then return that card to the battlefield tapped under its owner's control."
    }

    // Whenever an Equipment you control enters, draw a card.
    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Artifact.withSubtype("Equipment").youControl(),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.DrawCards(1)
        description = "Whenever an Equipment you control enters, draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/082cc8cc-bbea-4ca7-a0e8-da1f865d6626.jpg?1783902900"
    }
}
