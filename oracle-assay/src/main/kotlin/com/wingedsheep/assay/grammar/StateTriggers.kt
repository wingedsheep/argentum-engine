package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.StateTriggeredAbility
import com.wingedsheep.sdk.scripting.conditions.Condition

/**
 * "When you control no Islands, sacrifice ~." — a **state trigger** (CR 603.8): an ability that
 * triggers when a game state is true rather than when an event happens.
 *
 * A family of its own rather than a prefix in [Triggers], because the SDK gives it a type of its
 * own: `StateTriggeredAbility(condition, effect)` in `CardScript.stateTriggeredAbilities`, beside and
 * not inside the event-driven list. So the sentence is one template over two vocabularies the
 * grammar already has — [Conditions.condition] for the state, [Steps.step] for the payoff — and the
 * line lands in a fragment slot of its own, which is what [CardFragment.merge] and the
 * differential's modelled slots were widened for.
 *
 * Only "When" is read. Oracle also prints "Whenever there are four or more tide counters on ~"
 * (Homarid), and whether a state trigger is templated "when" or "whenever" says whether the effect
 * can make the state false again — nothing the model records. Both are the same type, so the second
 * spelling would be a second printed form for one model with nothing in the value to choose between
 * them; it declines until a card needs it and its reason can be stated.
 *
 * The payoff takes no target: `StateTriggeredAbility` has no requirement field, so a sentence whose
 * effect declares one is a value this type cannot hold, and it declines rather than dropping the
 * target. `activeZone` stays at its battlefield default for the same reason [Triggers] derives a
 * zone only from a printed return — no state trigger in the corpus functions anywhere else.
 */
object StateTriggers {

    /** One fixed id, as [Triggers] mints; the differential normalizes ability ids by position. */
    private val ID = AbilityId("state trigger")

    private fun abilityFor(condition: Condition, script: CardScript): StateTriggeredAbility? {
        if (script.targetRequirements.isNotEmpty()) return null
        val effect = script.spellEffect ?: return null
        return StateTriggeredAbility(id = ID, condition = condition, effect = effect)
    }

    val line: Phrase<StateTriggeredAbility> =
        phrase("when {condition}, {effect}", name = "a state-triggered ability") {
            slot("condition", Conditions.condition)
            slot("effect", Steps.step)
            build { abilityFor(it.value("condition"), it.value("effect")) }
            match { ability ->
                val script = CardScript(spellEffect = ability.effect)
                if (abilityFor(ability.condition, script)?.copy(id = ability.id) != ability) return@match null
                bind("condition" to ability.condition, "effect" to script)
            }
        }
}
