package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.model.Rarity

/**
 * Petrified Hamlet
 * Land
 *
 * When this land enters, choose a land card name.
 * Activated abilities of sources with the chosen name can't be activated unless they're mana abilities.
 * Lands with the chosen name have "{T}: Add {C}."
 * {T}: Add {C}.
 *
 * Modeling notes:
 * - "choose a land card name" is a name chosen *as the permanent enters* (worded "When this land
 *   enters" but mechanically an as-enters choice, like Sanctum Prelate / Pithing Needle naming).
 *   Modeled via [EntersWithChoice]`(ChoiceType.CARD_NAME)` — the engine presents every registered
 *   land card name and stores the pick durably on the permanent's `CastChoicesComponent` under
 *   `ChoiceSlot.CARD_NAME`.
 * - Both name-keyed static abilities read that durable choice through
 *   [GameObjectFilter.namedFromChosenComponent] (→ `CardPredicate.NameEqualsChosenComponent`),
 *   which is static-projection / activation-legality safe (it keys off the *source permanent's*
 *   choice rather than a transient pipeline variable). It fails closed before a name is chosen.
 * - "Activated abilities of sources with the chosen name can't be activated unless they're mana
 *   abilities" → [PreventActivatedAbilities]`(filter, nonManaAbilitiesOnly = true)`. "Sources" is
 *   any object, so the filter is the bare chosen-name predicate (not restricted to lands).
 * - "Lands with the chosen name have \"{T}: Add {C}.\"" → [GrantActivatedAbility] of a tap-for-{C}
 *   mana ability to the battlefield-scoped set of lands whose name matches the choice.
 * - "{T}: Add {C}." is the land's own intrinsic mana ability.
 */
val PetrifiedHamlet = card("Petrified Hamlet") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "When this land enters, choose a land card name.\n" +
        "Activated abilities of sources with the chosen name can't be activated unless they're mana abilities.\n" +
        "Lands with the chosen name have \"{T}: Add {C}.\"\n" +
        "{T}: Add {C}."

    // As this land enters, choose a land card name. Stored under ChoiceSlot.CARD_NAME.
    replacementEffect(EntersWithChoice(ChoiceType.CARD_NAME))

    // Activated abilities of sources with the chosen name can't be activated unless mana abilities.
    staticAbility {
        ability = PreventActivatedAbilities(
            filter = GameObjectFilter.Any.namedFromChosenComponent(),
            nonManaAbilitiesOnly = true,
        )
    }

    // Lands with the chosen name have "{T}: Add {C}."
    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = Costs.Tap,
                effect = Effects.AddColorlessMana(1),
                isManaAbility = true,
                timing = TimingRule.ManaAbility,
            ),
            filter = GroupFilter(GameObjectFilter.Land.namedFromChosenComponent()),
        )
    }

    // {T}: Add {C}.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "259"
        artist = "Richard Wright"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/355dd460-b0e9-41f2-a058-b7f7e39ac387.jpg?1777065626"
    }
}
