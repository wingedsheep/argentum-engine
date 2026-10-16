package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Harold and Bob, First Numens
 * {2}{G}
 * Legendary Creature — Treefolk Mutant
 * 3/3
 * Reach, vigilance
 * When Harold and Bob dies, if it was a creature, return it to the battlefield. It's an Aura
 * enchantment with enchant Forest you control and "Enchanted Forest has '{T}: Add three mana of any
 * one color. You get two rad counters.'" Harold and Bob loses all other abilities.
 *
 * The return is [Effects.PutOntoBattlefieldAsAura]: the card enters under this ability's
 * controller's control already a legendary Aura (no other types or subtypes), its controller
 * choosing which Forest it enchants as it enters; with no Forest to enchant it stays in the
 * graveyard (rulings). A token copy has ceased to exist by then and doesn't return.
 */
val HaroldAndBobFirstNumens = card("Harold and Bob, First Numens") {
    manaCost = "{2}{G}"
    typeLine = "Legendary Creature — Treefolk Mutant"
    power = 3
    toughness = 3
    oracleText = "Reach, vigilance\n" +
        "When Harold and Bob dies, if it was a creature, return it to the battlefield. It's an Aura " +
        "enchantment with enchant Forest you control and \"Enchanted Forest has '{T}: Add three mana of " +
        "any one color. You get two rad counters.'\" Harold and Bob loses all other abilities."

    keywords(Keyword.REACH, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.dies()
        interveningIf = Conditions.TriggeringEntityWas(GameObjectFilter.Creature)
        effect = Effects.PutOntoBattlefieldAsAura(
            enchant = GameObjectFilter.Land.withSubtype(Subtype.FOREST).youControl(),
            grantedStaticAbilities = listOf(
                GrantActivatedAbility(
                    ability = ActivatedAbility(
                        id = AbilityId.next(),
                        cost = Costs.Tap,
                        effect = Effects.AddAnyColorMana(3) then
                            Effects.AddCounters(CounterType.RAD, 2, EffectTarget.PlayerRef(Player.You)),
                        isManaAbility = true,
                        timing = TimingRule.ManaAbility,
                        descriptionOverride = "{T}: Add three mana of any one color. You get two rad counters."
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "78"
        artist = "Andrea Piparo"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d730ac69-20ec-4ef3-85ee-a5fd24e5f277.jpg?1783912398"
        ruling("2024-03-08", "If you control Harold and Bob, you'll return it to the battlefield when it dies while it's a creature. It doesn't matter who owns the card. You'll choose which Forest you control it will enchant as it returns to the battlefield.")
        ruling("2024-03-08", "If there's nothing for Harold and Bob to legally enchant, it remains in its owner's graveyard instead of returning to the battlefield.")
        ruling("2024-03-08", "A token that's a copy of Harold and Bob won't return to the battlefield from its owner's graveyard.")
        ruling("2024-03-08", "If a nontoken permanent that's a copy of Harold and Bob dies while it's a creature, it will return to the battlefield as an Aura with only the abilities granted by the effect that returned it to the battlefield. It will also retain its name, colors, and supertypes, but it will be an Aura enchantment with no other types or subtypes.")
    }
}
