package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Arna Kennerüd, Skycaptain
 * {2}{W}{U}{B}
 * Legendary Creature — Human Knight
 * 4/4
 *
 * Flying, lifelink
 * Ward—Discard a card.
 * Whenever a modified creature you control attacks, double the number of each kind of counter on
 * it. Then for each nontoken permanent attached to it, create a token that's a copy of that
 * permanent attached to that creature.
 *
 * The copies ride [Effects.CreateTokenCopyOfTarget]'s `attachedTo`: each enters already attached to
 * the attacker, so no host is chosen. An Aura copy that can't enchant it isn't created (CR 303.4i);
 * an Equipment copy that can't equip it enters unattached (CR 301.5e). The attachments are gathered
 * once, before any token exists, so the new token copies are never themselves copied.
 */
private val modifiedCreatureYouControl = GameObjectFilter.Creature.youControl().let {
    it.copy(statePredicates = it.statePredicates + StatePredicate.IsModified)
}

val ArnaKennerudSkycaptain = card("Arna Kennerüd, Skycaptain") {
    manaCost = "{2}{W}{U}{B}"
    colorIdentity = "WUB"
    typeLine = "Legendary Creature — Human Knight"
    power = 4
    toughness = 4
    oracleText = "Flying, lifelink\n" +
        "Ward—Discard a card.\n" +
        "Whenever a modified creature you control attacks, double the number of each kind of " +
        "counter on it. Then for each nontoken permanent attached to it, create a token that's a " +
        "copy of that permanent attached to that creature."

    keywords(Keyword.FLYING, Keyword.LIFELINK)
    keywordAbility(KeywordAbility.Ward(WardCost.Discard()))

    triggeredAbility {
        trigger = Triggers.a(modifiedCreatureYouControl).attacks()
        effect = Effects.DoubleAllCounters(EffectTarget.TriggeringEntity) then
            Effects.Pipeline {
                val attachments = gather(
                    CardSource.AttachedTo(
                        host = EffectTarget.TriggeringEntity,
                        filter = GameObjectFilter.Permanent.nontoken()
                    )
                )
                run(
                    Effects.ForEachInCollection(
                        attachments,
                        Effects.CreateTokenCopyOfTarget(
                            target = EffectTarget.IterationEntity,
                            attachedTo = EffectTarget.TriggeringEntity
                        )
                    )
                )
            }
        description = "Whenever a modified creature you control attacks, double the number of " +
            "each kind of counter on it. Then for each nontoken permanent attached to it, create a " +
            "token that's a copy of that permanent attached to that creature."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "178"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19266b5a-ff78-436c-b0f1-457e80bb647e.jpg?1783911254"
        ruling(
            "2024-06-07",
            "To double the number of each kind of counter on a permanent, put another counter on it for each counter it already has. Effects that interact with counters being put onto permanents, such as the effect of Branching Evolution, apply as appropriate."
        )
        ruling(
            "2024-06-07",
            "If, for any reason, an Aura token that would be created with Arna Kennerüd's last ability can't be attached to the appropriate creature, that token isn't created. If an Equipment token that can't be attached to the appropriate creature would be created, that token enters the battlefield unattached."
        )
        ruling(
            "2024-06-07",
            "An Aura controlled by another player does not cause a creature you control to be modified."
        )
        ruling(
            "2024-06-07",
            "A creature with a counter on it is considered modified no matter what kind of counter it is or which player put it on that creature."
        )
        ruling(
            "2024-06-07",
            "A creature that is equipped is considered modified no matter who controls the Equipment that's attached to it."
        )
    }
}
