package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * The Goose Mother
 * {X}{G}{U}
 * Legendary Creature — Bird Hydra
 * 2/2
 *
 * Flying
 * The Goose Mother enters with X +1/+1 counters on it.
 * When The Goose Mother enters, create half X Food tokens, rounded up.
 * Whenever The Goose Mother attacks, you may sacrifice a Food. If you do, draw a card.
 *
 * Both X clauses read [DynamicAmount.CastX], not [DynamicAmount.XValue]: the enters-with-counters
 * replacement and the enters trigger are two separate resolutions, and by the time the trigger
 * resolves the spell's transient resolution context is gone. `CastX` is the durable, object-scoped
 * reading that rides the spell's entity onto the battlefield, so the counters and the Food count
 * are guaranteed to agree on the same announced X (the Hydroid Krasis shape the type documents).
 *
 * "Half X, rounded up" is `Divide(CastX, 2, roundUp = true)` — X=0 makes no Food, X=1 makes one.
 *
 * The attack ability is [ProvisionsMerchant]'s exactly: a [MayEffect] over
 * `Sacrifice(Food).then(draw)`, so declining leaves the ability resolving harmlessly rather than
 * fizzling, and "If you do" (not "When you do") keeps the draw in the same resolution instead of a
 * reflexive trigger. The `feasibility` gate suppresses the prompt when the controller has no Food
 * — a flier that attacks every turn would otherwise raise an unanswerable question each combat.
 * "A Food" means any Food *artifact*, not just a Food token (2024-11-08 ruling), which is what the
 * subtype filter matches.
 */
val TheGooseMother = card("The Goose Mother") {
    manaCost = "{X}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Bird Hydra"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "The Goose Mother enters with X +1/+1 counters on it.\n" +
        "When The Goose Mother enters, create half X Food tokens, rounded up. (They're artifacts " +
        "with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "Whenever The Goose Mother attacks, you may sacrifice a Food. If you do, draw a card."

    keywords(Keyword.FLYING)

    replacementEffect(EntersWithDynamicCounters(count = DynamicAmount.CastX))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateFood(
            DynamicAmount.Divide(
                numerator = DynamicAmount.CastX,
                denominator = DynamicAmount.Fixed(2),
                roundUp = true,
            )
        )
        description = "When The Goose Mother enters, create half X Food tokens, rounded up."
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = MayEffect(
            Effects.Sacrifice(
                GameObjectFilter.Artifact.withSubtype("Food"),
                count = 1,
                target = EffectTarget.Controller,
            ).then(Effects.DrawCards(1)),
            feasibility = FeasibilityCheck.ControlsPermanentMatching(
                GameObjectFilter.Artifact.withSubtype("Food")
            ),
        )
        description = "You may sacrifice a Food. If you do, draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "204"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a55c370-d396-4c73-8ee2-83dc4c124005.jpg?1783915072"

        ruling(
            "2024-11-08",
            "If an effect refers to a Food, it means any Food artifact, not just a Food artifact " +
                "token. For example, you can sacrifice Tough Cookie (an Artifact Creature — Food " +
                "Golem) to activate an ability with \"Sacrifice a Food\" in its cost."
        )
        ruling(
            "2024-11-08",
            "Food is an artifact type. Even though it appears on some creatures, it's never a " +
                "creature type."
        )
        ruling(
            "2024-11-08",
            "You can't sacrifice a Food to pay multiple costs. For example, you can't sacrifice a " +
                "Food token to activate its own ability and also to activate this ability."
        )
    }
}
