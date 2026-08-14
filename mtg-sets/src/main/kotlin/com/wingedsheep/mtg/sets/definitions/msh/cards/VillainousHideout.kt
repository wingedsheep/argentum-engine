package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Villainous Hideout
 * Land
 * {T}: Add {C}.
 * {T}: Add one mana of any color. Spend this mana only to cast a Villain spell or to activate an
 * ability of a Villain source.
 * {3}, {T}: Target Villain you control connives. Activate only as a sorcery.
 *
 * Implementation note: the filtered mana is the Unclaimed Territory shape —
 * [ManaRestriction.SubtypeSpellsOrAbilitiesOnly] with `creatureOnly = false`, covering both the
 * spell and the ability clause. The connive names its own permanent as the counter recipient, so
 * it's the plain [Effects.Connive] over a cast-time `target(...)` (not `ConniveTargeting`, which
 * picks a *different* recipient reflexively at resolution). "Activate only as a sorcery" is
 * [TimingRule.SorcerySpeed].
 */
val VillainousHideout = card("Villainous Hideout") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{T}: Add one mana of any color. Spend this mana only to cast a Villain spell or to " +
        "activate an ability of a Villain source.\n" +
        "{3}, {T}: Target Villain you control connives. Activate only as a sorcery. (Draw a " +
        "card, then discard a card. If you discarded a nonland card, put a +1/+1 counter on that " +
        "creature.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(
            amount = 1,
            restriction = ManaRestriction.SubtypeSpellsOrAbilitiesOnly("Villain", creatureOnly = false)
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        val villain = target(
            "target Villain you control",
            TargetCreature(filter = TargetFilter.CreatureYouControl.withSubtype(Subtype.VILLAIN))
        )
        effect = Effects.Connive(target = villain)
        timing = TimingRule.SorcerySpeed
        description = "Target Villain you control connives. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "276"
        artist = "Paulius Daščioras"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/822b0249-e1df-453d-8b60-75a5196ed818.jpg?1783902881"
    }
}
