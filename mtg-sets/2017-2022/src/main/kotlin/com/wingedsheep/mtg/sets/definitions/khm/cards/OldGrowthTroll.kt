package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Old-Growth Troll
 * {G}{G}{G}
 * Creature — Troll Warrior
 * 4/4
 * Trample
 * When Old-Growth Troll dies, if it was a creature, return it to the battlefield. It's an Aura
 * enchantment with enchant Forest you control and "Enchanted Forest has '{T}: Add {G}{G}' and
 * '{1}, {T}, Sacrifice this land: Create a tapped 4/4 green Troll Warrior creature token with
 * trample.'"
 *
 * Unlike Harold and Bob, the text doesn't strip its abilities, so the return keeps them
 * (`losesOtherAbilities = false`; the copy ruling: "both its own normal abilities and the two new
 * abilities"). As an Aura it never dies again — the intervening "if it was a creature" ends the loop.
 */
val OldGrowthTroll = card("Old-Growth Troll") {
    manaCost = "{G}{G}{G}"
    typeLine = "Creature — Troll Warrior"
    power = 4
    toughness = 4
    oracleText = "Trample\n" +
        "When Old-Growth Troll dies, if it was a creature, return it to the battlefield. It's an Aura " +
        "enchantment with enchant Forest you control and \"Enchanted Forest has '{T}: Add {G}{G}' and " +
        "'{1}, {T}, Sacrifice this land: Create a tapped 4/4 green Troll Warrior creature token with " +
        "trample.'\""

    keywords(Keyword.TRAMPLE)

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
                        effect = Effects.AddMana(Color.GREEN, 2),
                        isManaAbility = true,
                        timing = TimingRule.ManaAbility,
                        descriptionOverride = "{T}: Add {G}{G}."
                    )
                ),
                GrantActivatedAbility(
                    ability = ActivatedAbility(
                        id = AbilityId.next(),
                        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.SacrificeSelf),
                        effect = Effects.CreateToken(
                            power = 4,
                            toughness = 4,
                            colors = setOf(Color.GREEN),
                            creatureTypes = setOf("Troll", "Warrior"),
                            keywords = setOf(Keyword.TRAMPLE),
                            tapped = true
                        ),
                        descriptionOverride = "{1}, {T}, Sacrifice this land: Create a tapped 4/4 green " +
                            "Troll Warrior creature token with trample."
                    )
                )
            ),
            losesOtherAbilities = false
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "185"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b759b0f6-342c-4bba-89f1-8451835d8c45.jpg?1783928208"
        ruling("2021-02-05", "If you control but don't own Old-Growth Troll, you (not its owner) will return it to the battlefield when it dies. You'll choose which Forest you control it will enchant as it returns to the battlefield.")
        ruling("2021-02-05", "If there's nothing for Old-Growth Troll to legally enchant, it remains in its owner's graveyard.")
        ruling("2021-02-05", "If a token is a copy of Old-Growth Troll, it won't return from its owner's graveyard.")
        ruling("2021-02-05", "If a nontoken permanent that's a copy of Old-Growth Troll dies while it is a creature, it will return to the battlefield as an Aura with both its own normal abilities and the two new abilities granted by old-Growth Troll. The resulting permanent will also retain its name, colors, and supertypes, but it will be an Aura enchantment with no other types or subtypes.")
    }
}
