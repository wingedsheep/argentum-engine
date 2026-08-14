package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Docent of Perfection // Final Iteration (Eldritch Moon #56 — the card's earliest printing; also
 * reprinted in Shadows of the Past and Innistrad Remastered)
 * {3}{U}{U}
 * Creature — Insect Horror 5/4 // Creature — Eldrazi Insect 6/5
 *
 * Front — Docent of Perfection ({3}{U}{U}, Creature — Insect Horror, 5/4)
 *   Flying
 *   Whenever you cast an instant or sorcery spell, create a 1/1 blue Human Wizard creature token.
 *   Then if you control three or more Wizards, transform this creature.
 *
 * Back — Final Iteration (Creature — Eldrazi Insect, 6/5, colorless)
 *   Flying
 *   Wizards you control get +2/+1 and have flying.
 *   Whenever you cast an instant or sorcery spell, create a 1/1 blue Human Wizard creature token.
 *
 * Implementation:
 *  - Both faces share the same [Triggers.YouCastInstantOrSorcery] → [Effects.CreateToken] trigger;
 *    the front adds a [ConditionalEffect] on [Conditions.YouControlAtLeast]`(3, Wizard)` that flips
 *    it. The token is made *before* the count, so the Wizard it just created counts toward the
 *    three, and the flip only happens while this ability resolves (printed ruling: already
 *    controlling three Wizards doesn't transform it on its own).
 *  - The token's art comes from Eldritch Moon's synced token set (the 1/1 blue Human Wizard), so no
 *    explicit `imageUri` is needed.
 *  - The back's anthem is two static rows over the same [GroupFilter] — [ModifyStats]`(2, 1)` in
 *    Layer 7c and [GrantKeyword]`(FLYING)` in Layer 6 — which the engine ties together with a shared
 *    `groupId` so both halves see the same affected set (CR 613.6). It covers *every* Wizard you
 *    control, not just the tokens; Final Iteration is an Eldrazi Insect, so it never pumps itself.
 */

private val WizardsYouControl: GroupFilter =
    GroupFilter(GameObjectFilter.Creature.youControl().withSubtype("Wizard"))

private val HumanWizardToken = Effects.CreateToken(
    power = 1,
    toughness = 1,
    colors = setOf(Color.BLUE),
    creatureTypes = setOf("Human", "Wizard"),
)

private val DocentOfPerfectionFront = card("Docent of Perfection") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Insect Horror"
    power = 5
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever you cast an instant or sorcery spell, create a 1/1 blue Human Wizard creature " +
        "token. Then if you control three or more Wizards, transform this creature."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YouCastInstantOrSorcery
        effect = Effects.Composite(
            HumanWizardToken,
            ConditionalEffect(
                condition = Conditions.YouControlAtLeast(
                    3,
                    GameObjectFilter.Creature.withSubtype("Wizard"),
                ),
                effect = TransformEffect(EffectTarget.Self),
            ),
        )
        description = "Create a 1/1 blue Human Wizard creature token. Then if you control three " +
            "or more Wizards, transform this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "56"
        artist = "Nils Hamm"
        flavorText = "The time had come for him to share his findings."
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30c3d4c1-dc3d-4529-9d6e-8c16149cf6da.jpg?1783937509"
        ruling(
            "2025-01-24",
            "An ability that triggers when a player casts a spell resolves before the spell that " +
                "caused it to trigger. It resolves even if that spell is countered or otherwise " +
                "leaves the stack without resolving."
        )
        ruling(
            "2025-01-24",
            "If you control three or more Wizards while you control Docent of Perfection, it won't " +
                "transform yet. It only transforms while its triggered ability is resolving after " +
                "you cast an instant or sorcery spell."
        )
        ruling(
            "2025-01-24",
            "When Docent of Perfection transforms into Final Iteration, the instant or sorcery " +
                "spell that's on the stack doesn't cause Final Iteration's triggered ability to " +
                "trigger."
        )
        ruling(
            "2025-01-24",
            "All Wizards you control get +2/+1 and have flying, not just those created by Docent " +
                "of Perfection or Final Iteration."
        )
    }
}

private val FinalIteration = card("Final Iteration") {
    manaCost = ""
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Insect"
    power = 6
    toughness = 5
    oracleText = "Flying\n" +
        "Wizards you control get +2/+1 and have flying.\n" +
        "Whenever you cast an instant or sorcery spell, create a 1/1 blue Human Wizard creature token."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ModifyStats(powerBonus = 2, toughnessBonus = 1, filter = WizardsYouControl)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, WizardsYouControl)
    }

    triggeredAbility {
        trigger = Triggers.YouCastInstantOrSorcery
        effect = HumanWizardToken
        description = "Create a 1/1 blue Human Wizard creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "56"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/back/3/0/30c3d4c1-dc3d-4529-9d6e-8c16149cf6da.jpg?1783937509"
    }
}

val DocentOfPerfection: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = DocentOfPerfectionFront,
    backFace = FinalIteration,
)
