package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mercurial Pretender — {4}{U} Creature — Shapeshifter 0/0.
 *
 * The standard [EntersAsCopy] clone replacement restricted to a creature *you control* (Mirror
 * Image's filter), with the "{2}{U}{U}: Return this creature to its owner's hand" ability riding
 * [CopyExceptions.addedActivatedAbilities] so it is a copiable value (Gigantoplasm's shape).
 * Declining the copy leaves a 0/0 with no ability, which state-based actions put into the graveyard.
 */
val MercurialPretender = card("Mercurial Pretender") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Shapeshifter"
    power = 0
    toughness = 0
    oracleText = "You may have this creature enter as a copy of a creature you control, except it has \"{2}{U}{U}: Return this creature to its owner's hand.\""

    replacementEffect(
        EntersAsCopy(
            optional = true,
            copyFilter = GameObjectFilter.Creature.youControl(),
            exceptions = CopyExceptions(
                addedActivatedAbilities = listOf(
                    grantedActivatedAbility {
                        cost = Costs.Mana("{2}{U}{U}")
                        effect = Effects.Move(EffectTarget.Self, Zone.HAND)
                    }
                )
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "68"
        artist = "Izzy"
        flavorText = "The king went off to find himself. Imagine his terror when he succeeded."
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3160d28-230d-4bce-9050-6ab5895302ee.jpg?1783939190"
        ruling("2014-07-18", "The activated ability Mercurial Pretender gives itself becomes part of its copiable values. Unless the ability is overwritten by another copy effect, a creature that's a copy of Mercurial Pretender will have that ability.")
        ruling("2014-07-18", "If the chosen creature is copying something else (for example, if the chosen creature is another Mercurial Pretender), then Mercurial Pretender enters the battlefield as whatever the chosen creature is copying (plus any abilities it gained as part of the copying process, if applicable).")
        ruling("2014-07-18", "If Mercurial Pretender somehow enters the battlefield at the same time as another creature, Mercurial Pretender can't become a copy of that creature. You may choose only a creature that's already on the battlefield.")
        ruling("2014-07-18", "You can choose not to copy anything. In that case, Mercurial Pretender enters the battlefield as a 0/0 Shapeshifter creature, and is probably put into the graveyard immediately.")
    }
}
