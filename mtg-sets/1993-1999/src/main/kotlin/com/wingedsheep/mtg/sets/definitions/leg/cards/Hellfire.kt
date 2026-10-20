package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hellfire
 * {2}{B}{B}{B}
 * Sorcery
 * Destroy all nonblack creatures. Hellfire deals X plus 3 damage to you, where X is the number of
 * creatures that died this way.
 *
 * "Died this way" counts creatures *put into a graveyard* by the destruction, not merely destroyed:
 * the destroyed collection already drops regenerated and indestructible creatures, and the
 * `currentlyIn(GRAVEYARD)` filter then drops any a replacement effect sent elsewhere (exile, say).
 * Destroyed tokens sit in the graveyard until state-based actions remove them, so they count.
 * The damage is one X+3 event, not 3 plus X separate hits.
 */
val Hellfire = card("Hellfire") {
    manaCost = "{2}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy all nonblack creatures. Hellfire deals X plus 3 damage to you, where X is the number of creatures that died this way."

    spell {
        effect = Effects.Pipeline {
            val destroyed = runStoringCollection {
                Effects.DestroyAll(GameObjectFilter.Creature.notColor(Color.BLACK), storeDestroyedAs = it)
            }
            val died = filter(destroyed, GameObjectFilter.Any.currentlyIn(Zone.GRAVEYARD))
            run(Effects.DealDamage(died.count + 3, EffectTarget.Controller))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "105"
        artist = "Pete Venters"
        flavorText = "\"High on a throne of royal state . . . insatiate to pursue vain war with heav'n.\" —John Milton, *Paradise Lost*"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/362f1fe9-20af-434c-9957-7a1a564d89e6.jpg?1783948065"
        ruling("2009-10-01", "Hellfire checks the number of creatures put into graveyards as the result of its effect, not the number of creatures destroyed as a result of its effect. For example, a replacement effect that says \"If a creature would be put into a graveyard from the battlefield, exile it instead\" won't affect whether a creature is destroyed or not, but will affect whether it's put into a graveyard or not.")
        ruling("2009-10-01", "A creature that regenerates instead of being destroyed doesn't leave the battlefield, so it's not put into a graveyard.")
        ruling("2009-10-01", "A token creature that's destroyed is put into its owner's graveyard, and will be counted by Hellfire's effect. (Immediately after Hellfire finishes resolving, the token will cease to exist.)")
    }
}
