package com.wingedsheep.sdk.scripting.costs

import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a cost that can be paid in various contexts:
 * - Morph face-up costs (e.g., "Morph {2}{U}" or "Morph—Pay 5 life")
 * - "Unless" mechanics via PayOrSufferEffect (e.g., "unless you sacrifice a creature")
 * - Any future mechanic that requires a payable cost
 *
 * Most payable things — mana, life, sacrifice, discard, exile, tap, return, reveal — are shared with the
 * other cost contexts and live in the [CostAtom] vocabulary; [Atom] carries one of them. The two members
 * left on this wrapper are the ones genuinely specific to *this* context: [OwnManaCost] (resolved against
 * the source permanent's own printed cost at payment time) and [Choice] (a player picks one of several
 * payable costs to satisfy).
 */
@Serializable
sealed interface PayCost : TextReplaceable<PayCost> {
    val description: String

    /**
     * A single shared payable thing — see [CostAtom]. The common case: "Morph {2}{U}", "unless you
     * sacrifice a creature", "unless you pay 3 life", etc. PayCost is used mid-sentence, so the
     * description is taken verbatim from the atom (lower-case-leading).
     */
    @SerialName("PayAtom")
    @Serializable
    data class Atom(val atom: CostAtom) : PayCost {
        override val description: String get() = atom.description

        override fun applyTextReplacement(replacer: TextReplacer): PayCost {
            val newAtom = atom.applyTextReplacement(replacer)
            return if (newAtom !== atom) copy(atom = newAtom) else this
        }
    }

    /**
     * Pay the mana cost of the permanent the cost applies to (its own mana cost).
     *
     * Resolved at payment time by reading the source permanent's `CardComponent.manaCost`,
     * so it works for granted abilities like Essence Leak ("...sacrifice this permanent
     * unless you pay its mana cost"), where the affected permanent — not a fixed cost — owns
     * the mana cost. The engine converts this into a concrete [CostAtom.Mana] cost against that
     * permanent before prompting.
     */
    @SerialName("OwnManaCost")
    @Serializable
    data object OwnManaCost : PayCost {
        override val description: String = "its mana cost"
    }

    /**
     * Choose one of several costs to pay.
     * "...unless they sacrifice a nonland permanent or discard a card"
     *
     * The player chooses which cost to pay (or accepts the consequence).
     *
     * @property options The available cost options
     */
    @SerialName("Choice")
    @Serializable
    data class Choice(
        val options: List<PayCost>
    ) : PayCost {
        override val description: String get() = options.joinToString(" or ") { it.description }
        override fun applyTextReplacement(replacer: TextReplacer): PayCost {
            val newOptions = options.map { it.applyTextReplacement(replacer) }
            return if (newOptions.zip(options).any { (a, b) -> a !== b }) copy(options = newOptions) else this
        }
    }
}
