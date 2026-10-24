package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hofri Ghostforge — Strixhaven: School of Mages #192 (canonical printing)
 * {3}{R}{W} · Legendary Creature — Dwarf Cleric · 4/5
 *
 * The exiled card is filed in each Spirit token's own linked-exile pile, so the token's granted
 * leaves trigger returns exactly that card (and nothing else Hofri exiled). A copy of the token
 * gets the ability but an empty pile, so it returns nothing — both per the card's rulings. The
 * copy reads the creature as it last existed on the battlefield (the dies trigger's snapshot),
 * not the card in exile.
 */
private val spirits = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.SPIRIT).youControl())

val HofriGhostforge = card("Hofri Ghostforge") {
    manaCost = "{3}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Dwarf Cleric"
    power = 4
    toughness = 5
    oracleText = "Spirits you control get +1/+1 and have trample and haste.\n" +
        "Whenever another nontoken creature you control dies, exile it. If you do, create a token " +
        "that's a copy of that creature, except it's a Spirit in addition to its other types and it " +
        "has \"When this token leaves the battlefield, return the exiled card to its owner's graveyard.\""

    staticAbility {
        ability = ModifyStats(powerBonus = 1, toughnessBonus = 1, filter = spirits)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, spirits)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, spirits)
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl().nontoken()).dies()
        effect = Effects.IfYouDo(
            action = Effects.Pipeline {
                exile(gather(CardSource.Entity(EffectTarget.TriggeringEntity)))
            },
            then = Effects.Pipeline {
                run(
                    Effects.CreateTokenCopyOfTarget(
                        EffectTarget.TriggeringEntity,
                        exceptions = CopyExceptions(
                            addedSubtypes = setOf(Subtype.SPIRIT),
                            addedTriggeredAbilities = listOf(
                                grantedTriggeredAbility {
                                    trigger = Triggers.self.leaves()
                                    effect = Effects.Pipeline {
                                        move(gather(CardSource.FromLinkedExile()), CardDestination.ToZone(Zone.GRAVEYARD))
                                    }
                                    description = "When this token leaves the battlefield, return the exiled " +
                                        "card to its owner's graveyard."
                                }
                            ),
                        ),
                    )
                )
                val exiled = gather(CardSource.Entity(EffectTarget.TriggeringEntity))
                run(
                    Effects.ForEachInCollection(
                        CollectionSlot.CreatedTokens,
                        Effects.LinkCollectionToPermanent(exiled, EffectTarget.IterationEntity),
                    )
                )
            },
        )
        description = "Whenever another nontoken creature you control dies, exile it. If you do, create a " +
            "token that's a copy of that creature, except it's a Spirit in addition to its other types and " +
            "it has \"When this token leaves the battlefield, return the exiled card to its owner's graveyard.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "192"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd8deecf-eded-418b-ac4c-5c3e8f54e86d.jpg?1783927311"
        ruling("2021-04-16", "If you can't exile the card, perhaps because another effect moved the card before the triggered ability started resolving, you won't create a token.")
        ruling("2021-04-16", "Hofri Ghostforge's last ability is linked to the ability it grants the Spirit token. If a replacement effect causes you to create more than one Spirit token with this effect, they will all have that ability, and the exiled card will be returned to the graveyard if any of them leaves the battlefield.")
        ruling("2021-04-16", "Each Spirit token you create refers only to the card you exile during the resolution of the ability that created that token. When it leaves the battlefield, you return only that card to your graveyard, not any other card exiled by Hofri.")
        ruling("2021-04-16", "If another creature enters the battlefield as a copy of the Spirit token or another effect creates a copy of the Spirit token, it will have the triggered ability granted by Hofri, but will not be linked to Hofri's ability and will not return the card to the graveyard when it leaves the battlefield.")
        ruling("2021-04-16", "The copy uses the copiable values of the creature as it last existed on the battlefield before it died, not as it existed in the graveyard before it was exiled.")
    }
}
