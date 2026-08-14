package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Living Lectern
 * {1}{U}
 * Artifact Creature — Construct
 * 0/4
 *
 * {1}, Sacrifice this creature: Draw a card. Create a Sorcerer Role token attached to up to one
 * other target creature you control. Activate only as a sorcery.
 *
 * "Up to one other target" is `optional = true` on [TargetFilter.OtherCreatureYouControl] — the
 * "other" is the `excludeSelf` flag on that filter, and it is load-bearing even though the Lectern
 * sacrifices itself: targets are chosen (CR 601.2c) *before* costs are paid (CR 601.2h), so the
 * Lectern is still on the battlefield and would otherwise be a legal choice for its own Role.
 *
 * The draw and the Role are one effect sequence, not two independent ones, and that is exactly the
 * asymmetry the WOE rulings call out: activating with **no** target still draws, but choosing a
 * target that has become illegal by resolution means the whole ability doesn't resolve (CR 608.2b) —
 * no Role *and* no card. Modelling this as `Draw.then(CreateRoleToken)` behind a single optional
 * target requirement gets both halves for free rather than special-casing the fizzle.
 */
val LivingLectern = card("Living Lectern") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Construct"
    power = 0
    toughness = 4
    oracleText = "{1}, Sacrifice this creature: Draw a card. Create a Sorcerer Role token attached " +
        "to up to one other target creature you control. Activate only as a sorcery. (If you " +
        "control another Role on it, put that one into the graveyard. Enchanted creature gets +1/+1 " +
        "and has \"Whenever this creature attacks, scry 1.\")"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.SacrificeSelf)
        timing = TimingRule.SorcerySpeed
        val host = target(
            "up to one other target creature you control",
            TargetCreature(optional = true, filter = TargetFilter.OtherCreatureYouControl),
        )
        effect = Effects.DrawCards(1).then(Effects.CreateRoleToken("Sorcerer Role", host))
        description = "Draw a card. Create a Sorcerer Role token attached to up to one other " +
            "target creature you control."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "59"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/1/6/169afcaf-7ebf-4590-9e51-2a1a5eb3ac76.jpg?1783915118"

        ruling(
            "2023-09-01",
            "You can activate Living Lectern's ability without a target just to draw a card. However, " +
                "if you do choose a target, and that target is illegal at the time the ability tries " +
                "to resolve, the ability won't resolve and none of its effects will happen. You won't " +
                "draw a card."
        )
        ruling(
            "2023-09-01",
            "If you don't choose a target creature, the Sorcerer Role token won't be created."
        )
        ruling(
            "2023-09-01",
            "Roles are colorless enchantment tokens. Each one has the Aura and Role subtypes and the " +
                "enchant creature ability."
        )
    }
}
