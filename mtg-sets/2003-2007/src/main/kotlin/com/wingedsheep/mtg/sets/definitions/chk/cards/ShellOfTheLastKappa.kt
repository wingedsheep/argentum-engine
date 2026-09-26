package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Shell of the Last Kappa
 * {3}
 * Legendary Artifact
 * {3}, {T}: Exile target instant or sorcery spell that targets you. (The spell has no effect.)
 * {3}, {T}, Sacrifice Shell of the Last Kappa: You may cast a spell from among cards exiled with
 * Shell of the Last Kappa without paying its mana cost.
 *
 * The first ability is a non-counter exile ([Effects.ExileTargetSpell], so it also takes spells
 * that "can't be countered") into this artifact's linked-exile pile. "That targets you" is
 * `targetsPlayer(Player.You)`, read relative to the ability's controller.
 *
 * The second ability sacrifices the Shell as a cost; the pile is still read afterwards through the
 * departed-source linked-exile record. Choosing zero cards is the "may" decline.
 */
val ShellOfTheLastKappa = card("Shell of the Last Kappa") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "{3}, {T}: Exile target instant or sorcery spell that targets you. (The spell has no effect.)\n" +
        "{3}, {T}, Sacrifice Shell of the Last Kappa: You may cast a spell from among cards exiled with " +
        "Shell of the Last Kappa without paying its mana cost."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        target(TargetFilter.InstantOrSorcerySpellOnStack.targetsPlayer(Player.You))
        effect = Effects.ExileTargetSpell(linkToSource = true)
        description = "Exile target instant or sorcery spell that targets you."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Pipeline {
            val shellExiled = gather(CardSource.FromLinkedExile())
            val shellChosen = chooseUpTo(
                1,
                from = shellExiled,
                prompt = "Choose a card exiled with Shell of the Last Kappa to cast without paying its mana cost"
            )
            run(Effects.CastFromCollectionWithoutPayingCost(from = shellChosen))
        }
        description = "You may cast a spell from among cards exiled with Shell of the Last Kappa " +
            "without paying its mana cost."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "269"
        artist = "David Martin"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d80f3e7-c3f0-462f-8ae9-8b27a7c15fcd.jpg?1783944275"

        ruling("2004-12-01", "Shell of the Last Kappa's first ability can only target a spell that targets you.")
        ruling("2004-12-01", "Exiling a spell prevents that spell from resolving, but it doesn't technically \"counter\" anything. This means that Shell of the Last Kappa can exile spells which \"can't be countered.\"")
    }
}
