package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.teamwork
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Cruel Alliance — Marvel Super Heroes #92
 * {2}{B} · Sorcery
 *
 * Teamwork 2 (As an additional cost to cast this spell, you may tap any number of creatures you
 * control with total power 2 or more.)
 * Exile target creature with mana value 3 or less. If this spell was cast using teamwork, instead
 * exile target creature and you gain 3 life.
 *
 * The **teamwork-only targeting** shape of teamwork: "instead" here replaces the whole sentence,
 * target restriction included, so this is not a rider on one effect — the declared cast announces a
 * *different* target requirement. CR 601.2c is the rules basis ("a spell may require alternative
 * targets only if an alternative or additional cost was chosen for it"); CR 702.194c supplies the
 * other direction, that the plain cast is announced as though the teamwork clause's target weren't
 * there. That maps onto the shared optional-additional-cost rail's `kickerTarget` / `kickerEffect`
 * slots (Fight with Fire, Brave the Wilds), which serve whichever mechanic declared — teamwork
 * here. The plain cast can only ever announce a creature with mana value 3 or less; the teamwork
 * cast announces any creature and cannot be announced at all unless a creature is there to target.
 *
 * Because the branch replaces the effect wholesale, the teamwork branch restates the exile
 * alongside the life gain.
 */
val CruelAlliance = card("Cruel Alliance") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Teamwork 2 (As an additional cost to cast this spell, you may tap any number of " +
        "creatures you control with total power 2 or more.)\n" +
        "Exile target creature with mana value 3 or less. If this spell was cast using teamwork, " +
        "instead exile target creature and you gain 3 life."

    teamwork(2)

    spell {
        val small = target(
            "target creature with mana value 3 or less",
            TargetCreature(filter = TargetFilter.Creature.manaValueAtMost(3)),
        )
        effect = Effects.Exile(small)

        val anyCreature = kickerTarget(
            "target creature",
            Targets.Creature,
        )
        kickerEffect = Effects.Composite(
            Effects.Exile(anyCreature),
            Effects.GainLife(3),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "92"
        artist = "Vilhelmas Banys"
        flavorText = "\"Die, you pathetic swine!\"\n—Viper, Ophelia Sarkissian"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d895d5a1-d382-438b-8551-e142bb5142af.jpg?1783902948"
    }
}
