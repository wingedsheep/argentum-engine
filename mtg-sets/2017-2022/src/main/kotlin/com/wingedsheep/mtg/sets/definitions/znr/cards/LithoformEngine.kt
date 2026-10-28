package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lithoform Engine — Zendikar Rising #245
 * {4} · Legendary Artifact
 *
 * {2}, {T}: Copy target activated or triggered ability you control. You may choose new targets for the copy.
 * {3}, {T}: Copy target instant or sorcery spell you control. You may choose new targets for the copy.
 * {4}, {T}: Copy target permanent spell you control. (The copy becomes a token.)
 *
 * The third ability has no "you may choose new targets": a copied Aura or mutating creature spell
 * keeps its original target (ruling), so it copies with `mayChooseNewTargets = false`.
 */
val LithoformEngine = card("Lithoform Engine") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "{2}, {T}: Copy target activated or triggered ability you control. You may choose new targets for the copy.\n" +
        "{3}, {T}: Copy target instant or sorcery spell you control. You may choose new targets for the copy.\n" +
        "{4}, {T}: Copy target permanent spell you control. (The copy becomes a token.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val ability = target(TargetFilter.ActivatedOrTriggeredAbilityOnStack.youControl())
        effect = Effects.CopyTargetSpellOrAbility(ability)
        description = "{2}, {T}: Copy target activated or triggered ability you control. You may choose new targets for the copy."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        val spell = target(TargetFilter.InstantOrSorcerySpellOnStack.youControl())
        effect = Effects.CopyTargetSpell(target = spell)
        description = "{3}, {T}: Copy target instant or sorcery spell you control. You may choose new targets for the copy."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        val spell = target(TargetFilter(GameObjectFilter.Permanent, zone = Zone.STACK).youControl())
        effect = Effects.CopyTargetSpell(target = spell, mayChooseNewTargets = false)
        description = "{4}, {T}: Copy target permanent spell you control. (The copy becomes a token.)"
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "245"
        artist = "Colin Boyer"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/6683416a-5820-4cd0-b28a-60a53239e9ef.jpg?1783929315"
        ruling(
            "2020-09-25",
            "Lithoform Engine can copy any spell or ability on the stack, not just one with targets. If a permanent " +
                "spell is copied, new targets can't be chosen for it, if it has any (perhaps because it's an Aura or a " +
                "mutating creature spell)."
        )
        ruling(
            "2020-09-25",
            "If a permanent spell is copied, it's put onto the battlefield as a token as the spell resolves rather " +
                "than putting the copy of the spell onto the battlefield."
        )
    }
}
