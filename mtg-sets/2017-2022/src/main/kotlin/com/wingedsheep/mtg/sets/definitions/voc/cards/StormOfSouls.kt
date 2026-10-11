package com.wingedsheep.mtg.sets.definitions.voc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Storm of Souls
 * {4}{W}{W}
 * Sorcery
 *
 * Return all creature cards from your graveyard to the battlefield. Each of them is a 1/1 Spirit
 * with flying in addition to its other types. Exile Storm of Souls.
 *
 * Same shape as Ghost Vacuum: gather your graveyard's creature cards, move them onto the
 * battlefield tracking which actually arrived, then iterate the moved collection setting base P/T
 * to 1/1, adding the Spirit creature type and granting flying — all permanent. `selfExile()`
 * sends the spell to exile instead of the graveyard as it finishes resolving.
 */
val StormOfSouls = card("Storm of Souls") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Return all creature cards from your graveyard to the battlefield. Each of them is " +
        "a 1/1 Spirit with flying in addition to its other types. Exile Storm of Souls."

    spell {
        effect = Effects.Pipeline {
            val creatures = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.You,
                    filter = GameObjectFilter.Creature
                )
            )
            val returned = moveTracked(creatures, CardDestination.ToZone(Zone.BATTLEFIELD))
            run(
                Effects.ForEachInCollection(
                    returned,
                    Effects.SetBasePowerAndToughness(
                        power = 1,
                        toughness = 1,
                        target = EffectTarget.IterationEntity,
                        duration = Duration.Permanent,
                    ) then
                        Effects.AddCreatureType("Spirit", EffectTarget.IterationEntity) then
                        Effects.GrantKeyword(Keyword.FLYING, EffectTarget.IterationEntity, Duration.Permanent),
                )
            )
        }
        selfExile()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "Liiga Smilshkalne"
        flavorText = "Devotion does not end with death."
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30cb1c3f-19d4-47d3-960e-f2de828e6feb.jpg?1783925007"
    }
}
