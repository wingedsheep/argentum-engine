package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.PayManaCostEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ant-Man, Colony Commander — Marvel Super Heroes #201 (uncommon)
 * {1}{G}{U} · Legendary Creature — Human Rogue Hero · 2/2
 *
 * Whenever Ant-Man attacks, you may pay {1}. When you do, put a +1/+1 counter on target creature.
 * Whenever you put a +1/+1 counter on a creature, create a 1/1 green Insect creature token. This
 * ability triggers only once each turn.
 *
 * Implementation notes:
 * - "You may pay {1}. When you do, …" is a reflexive trigger (CR 603.11), not a modal or an
 *   additional cost: the attack trigger resolves, offers the payment, and only on payment does a
 *   *second* ability go on the stack — targeting a creature as it does so, chosen after the
 *   payment. [ReflexiveTriggerEffect] with `action = PayManaCostEffect({1})` and
 *   `reflexiveTargetRequirements = [Targets.Creature]` is that shape (Thousand Moons Crackshot).
 *   Modelling it as an optional targeted trigger would wrongly lock the target in before the
 *   payment decision.
 * - "Target creature" is unrestricted — you may put the counter on an opponent's creature.
 * - The second ability is the Knight of Wundagore shape. The "you put" scope comes from
 *   `placedBy = Player.You` (the *placer*, CR 122.6a), not from the recipient filter: the printed
 *   text says "a creature", not "a creature you control", so counters you put on an opponent's
 *   creature count too. [TriggerBinding.ANY] (not `OTHER`) because the text has no "another" —
 *   a counter landing on Ant-Man himself fires it, which is exactly what the first ability's
 *   reflexive trigger can do.
 * - `firstTimeEachTurn = false` because the printed cap is on the *ability*, not on the recipient
 *   permanent; the trigger's own per-permanent first-placement gate would let a second creature
 *   fire it again the same turn. `oncePerTurn = true` is the engine's tracker for the printed
 *   "This ability triggers only once each turn" clause.
 * - The two abilities chain: paying {1} on the attack puts a counter, which fires the Insect
 *   trigger (once per turn).
 */
val AntManColonyCommander = card("Ant-Man, Colony Commander") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Human Rogue Hero"
    power = 2
    toughness = 2
    oracleText = "Whenever Ant-Man attacks, you may pay {1}. When you do, put a +1/+1 counter on " +
        "target creature.\n" +
        "Whenever you put a +1/+1 counter on a creature, create a 1/1 green Insect creature " +
        "token. This ability triggers only once each turn."

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = ReflexiveTriggerEffect(
            // "you may pay {1}"
            action = PayManaCostEffect(ManaCost.parse("{1}")),
            optional = true,
            // "When you do, put a +1/+1 counter on target creature."
            reflexiveEffect = Effects.AddCounters(
                Counters.PLUS_ONE_PLUS_ONE,
                1,
                EffectTarget.ContextTarget(0),
            ),
            reflexiveTargetRequirements = listOf(Targets.Creature),
        )
        description = "Whenever Ant-Man attacks, you may pay {1}. When you do, put a +1/+1 " +
            "counter on target creature."
    }

    triggeredAbility {
        trigger = Triggers.countersPlacedOn(
            filter = GameObjectFilter.Creature,
            counterType = Counters.PLUS_ONE_PLUS_ONE,
            firstTimeEachTurn = false,
            binding = TriggerBinding.ANY,
            placedBy = Player.You,
        )
        oncePerTurn = true
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf(Subtype.INSECT.value),
            imageUri = "https://cards.scryfall.io/normal/front/e/5/e5aa36ec-5f3a-405d-9a65-5a56a44dcee3.jpg?1783902801",
        )
        description = "Whenever you put a +1/+1 counter on a creature, create a 1/1 green Insect " +
            "creature token. This ability triggers only once each turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "201"
        artist = "Nathaniel Himawan"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26faf2db-ad86-462f-b61f-c1893c9aebbf.jpg?1783902906"
    }
}
