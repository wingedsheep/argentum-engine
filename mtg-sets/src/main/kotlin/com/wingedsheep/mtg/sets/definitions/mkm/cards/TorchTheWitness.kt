package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Torch the Witness — Murders at Karlov Manor #146
 * {X}{R} · Sorcery · Uncommon
 *
 * Torch the Witness deals twice X damage to target creature. If excess damage was dealt to that
 * creature this way, investigate.
 *
 * A scaling burn spell that rewards *overkill*: X = 2 kills a 3-toughness creature and, because the
 * fourth point of damage was excess, also leaves a Clue behind. Paying one more than you need is
 * how you turn removal into removal-plus-a-card.
 *
 * Composed entirely from existing atoms, the same shape as
 * [com.wingedsheep.mtg.sets.definitions.eoe.cards.OrbitalPlunge]:
 *
 * - **"twice X"** is [DynamicAmount.Multiply] over [DynamicAmount.XValue] — the chosen value of X,
 *   read from the resolution context, doubled. It is deliberately *not* a `{X}{X}` mana cost: the
 *   card costs X once and deals double, so a 4-mana cast (X = 3) deals 6.
 * - **"if excess damage was dealt … this way"** is [Conditions.IfTargetTookExcessDamage], which
 *   reads the post-damage marked-damage state on the target rather than needing a bespoke
 *   excess-damage executor. Per the printed ruling, excess is measured against the *lethal*
 *   threshold — damage already marked on the creature this turn counts, so 2 damage into a 4/4 that
 *   was already dealt 3 is excess; and if this spell somehow has deathtouch, any amount over 1 is.
 *   Reading live state is what makes both cases fall out for free.
 *
 * X = 0 is a legal cast: it deals 0 damage, no damage is excess, and no Clue is created. The
 * conditional is a plain gate rather than a reflexive trigger because "investigate" here happens as
 * part of this spell's resolution — there is no "when you do" to put on the stack.
 */
val TorchTheWitness = card("Torch the Witness") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Torch the Witness deals twice X damage to target creature. If excess damage was " +
        "dealt to that creature this way, investigate. (Create a Clue token. It's an artifact " +
        "with \"{2}, Sacrifice this token: Draw a card.\")"

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.Composite(
            Effects.DealDamage(DynamicAmount.Multiply(DynamicAmount.XValue, 2), creature),
            ConditionalEffect(
                condition = Conditions.IfTargetTookExcessDamage(),
                effect = Effects.Investigate(),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "146"
        artist = "Andrew Mar"
        flavorText = "\"He was scheduled to meet me tomorrow with something important to share. " +
            "This was no coincidence.\"\n—Algrom of the Foundway Associates"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22bbf709-d8e9-4e3b-8ec8-206f1b2162b3.jpg?1783912874"

        ruling(
            "2024-02-02",
            "A creature has been dealt excess damage if one or more sources dealt more damage to " +
                "it than the minimum amount of damage required to be lethal damage. In most " +
                "cases, this means damage greater than its toughness, but consider the damage " +
                "already dealt to it this turn."
        )
        ruling(
            "2024-02-02",
            "Even 1 damage dealt to a creature from a source with deathtouch is considered lethal " +
                "damage. If Torch the Witness has deathtouch (perhaps due to the effect of " +
                "Judith, Carnage Connoisseur), any amount greater than 1 damage will cause excess " +
                "damage to be dealt, even if the total amount of damage dealt to that creature " +
                "this turn isn't greater than the creature's toughness."
        )
    }
}
