package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Granite Shard — Mirrodin #182 (canonical printing, only printing)
 * {3} · Artifact
 *
 * {3}, {T} or {R}, {T}: This artifact deals 1 damage to any target.
 *
 * The "{3}, {T} or {R}, {T}" template is one printed ability with two alternative cost sets, and the
 * 2004-10-04 ruling is explicit that you pay one or the other, never both. Two `activatedAbility`
 * blocks with identical effects model exactly that: each offers its own cost, and because both
 * include the shard's own `{T}` they compete for the same tap, so only one can be activated per
 * untap — the same play pattern as the single printed ability. (There is no "either cost" cost atom
 * in the SDK, and adding one would only re-derive this.)
 *
 * `colorIdentity = "R"` because the {R} alternative puts red in the card's identity even though the
 * card itself is colorless (CR 903.4).
 */
val GraniteShard = card("Granite Shard") {
    manaCost = "{3}"
    colorIdentity = "R"
    typeLine = "Artifact"
    oracleText = "{3}, {T} or {R}, {T}: This artifact deals 1 damage to any target."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        val victim = target("any target", AnyTarget())
        effect = Effects.DealDamage(1, victim)
        description = "{3}, {T}: This artifact deals 1 damage to any target."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.Tap)
        val victim = target("any target", AnyTarget())
        effect = Effects.DealDamage(1, victim)
        description = "{R}, {T}: This artifact deals 1 damage to any target."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "182"
        artist = "Doug Chaffee"
        flavorText = "It's a piece of a world the goblins have never seen but would dearly like to blow up."
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e197af6a-24ac-4f3b-ab3c-736f4057748b.jpg?1783944519"
        ruling("2004-10-04", "You can pay either of the two costs (but not both at the same time) to activate the ability.")
    }
}
