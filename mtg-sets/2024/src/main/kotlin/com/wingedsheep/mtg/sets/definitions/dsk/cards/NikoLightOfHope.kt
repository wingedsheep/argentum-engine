package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.scripting.effects.CopySourceRead
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Niko, Light of Hope — Duskmourn: House of Horror #224
 * {2}{W}{U} · Legendary Creature — Human Wizard · 3/4
 *
 * When Niko enters, create two Shard tokens. (They're enchantments with "{2}, Sacrifice this
 * token: Scry 1, then draw a card.")
 * {2}, {T}: Exile target nonlegendary creature you control. Shards you control become copies of it
 * until the next end step. Return it to the battlefield under its owner's control at the beginning
 * of the next end step.
 *
 * The Shard token is a predefined enchantment token ([Effects.CreateShard]) — the Clue token's
 * enchantment cousin (Scry 1 then draw rather than just draw).
 *
 * The activated ability composes three established pieces:
 *  1. [Patterns.Exile.exileUntilEndStep] — exile the target and schedule a delayed
 *     "return it at the beginning of the next end step" trigger (the same blink Liberate uses).
 *  2. [Effects.EachPermanentBecomesCopyOfTarget] with `sourceRead = CopySourceRead.ANY_ZONE` — the Shards
 *     (filtered by name) become copies of the just-exiled creature, reading its copiable values
 *     from exile (the same off-battlefield copy source Lazav, Familiar Stranger uses).
 *  3. [Duration.UntilNextEndStep] times the copies to wear off on entry to that same end step, so
 *     the source returns and the Shards revert to enchantments together (CR 707: copiable values
 *     only — the Shards keep their own counters/auras, of which they have none).
 *
 * The copy reads the exiled creature's characteristics as they last existed on the battlefield (a
 * creature already copying something is read in exile from its last on-battlefield card identity),
 * matching the printed rulings for the common case.
 */
val NikoLightOfHope = card("Niko, Light of Hope") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Wizard"
    power = 3
    toughness = 4
    oracleText = "When Niko enters, create two Shard tokens. (They're enchantments with \"{2}, " +
        "Sacrifice this token: Scry 1, then draw a card.\")\n" +
        "{2}, {T}: Exile target nonlegendary creature you control. Shards you control become " +
        "copies of it until the next end step. Return it to the battlefield under its owner's " +
        "control at the beginning of the next end step."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateShard(2)
        description = "When Niko enters, create two Shard tokens."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap
        )
        val nonlegendaryCreatureYouControl = target(TargetFilter(GameObjectFilter.Creature.nonlegendary().youControl()))
        effect = Patterns.Exile.exileUntilEndStep(nonlegendaryCreatureYouControl) then
            Effects.EachPermanentBecomesCopyOfTarget(
                target = nonlegendaryCreatureYouControl,
                filter = GroupFilter(GameObjectFilter.Any.named("Shard").youControl()),
                duration = Duration.UntilNextEndStep,
                sourceRead = CopySourceRead.ANY_ZONE,
            )
        description = "{2}, {T}: Exile target nonlegendary creature you control. Shards you " +
            "control become copies of it until the next end step. Return it to the battlefield " +
            "under its owner's control at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "224"
        artist = "Aurore Folny"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91ad013f-de8d-4980-b4b6-c7f91ff495b1.jpg?1726286704"

        ruling("2024-09-20", "If the target creature is an illegal target as Niko's last ability tries to resolve, it won't resolve and none of its effects will happen. Shards you control won't become copies of the creature.")
        ruling("2024-09-20", "If Shards you control do become copies of the creature, they'll use the copiable values of the creature as it last existed on the battlefield. They don't copy whether that creature was tapped or untapped, whether it had any counters on it, whether it had any Auras and/or Equipment attached to it, or any non-copy effects that changed its power, toughness, types, color, and so on.")
        ruling("2024-09-20", "If the creature that was exiled was copying something else, the Shards that become copies of that creature will use the copiable values of that creature. In most cases, they will be copies of whatever that creature was copying. If it was copying a permanent or card with {X} in its mana cost, X is 0.")
    }
}
