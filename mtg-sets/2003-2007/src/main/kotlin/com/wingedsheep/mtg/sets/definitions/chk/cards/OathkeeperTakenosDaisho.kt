package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Oathkeeper, Takeno's Daisho
 * {3}
 * Legendary Artifact — Equipment
 * Equipped creature gets +3/+1.
 * Whenever equipped creature dies, return that card to the battlefield under your control if
 * it's a Samurai card.
 * When Oathkeeper is put into a graveyard from the battlefield, exile equipped creature.
 * Equip {2}
 *
 * The return is Demonic Vigor's `Triggers.attached.dies()` with a resolution-time check on the
 * card itself: per the 2004-12-01 ruling, "Samurai card" is read off the card in the graveyard
 * (its printed types), not off the creature as it last existed on the battlefield, so the gate is
 * `EntityMatches(TriggeringEntity, Samurai)` rather than a last-known subtype test. The move is
 * graveyard-guarded — a card that left the graveyard before resolution is a new object.
 *
 * The exile rides `Triggers.self.dies()` (the noncreature "put into a graveyard from the
 * battlefield" wording is the same event). "Equipped creature" is resolved by
 * [EffectTarget.EquippedCreature], which falls back to the Equipment's last-known attachment
 * (CR 603.10 look-back) once it has left the battlefield. If Oathkeeper wasn't attached to
 * anything, the ability exiles nothing.
 */
val OathkeeperTakenosDaisho = card("Oathkeeper, Takeno's Daisho") {
    manaCost = "{3}"
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Equipped creature gets +3/+1.\n" +
        "Whenever equipped creature dies, return that card to the battlefield under your control " +
        "if it's a Samurai card.\n" +
        "When Oathkeeper is put into a graveyard from the battlefield, exile equipped creature.\n" +
        "Equip {2}"

    staticAbility {
        ability = ModifyStats(3, 1)
    }

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(
                GameObjectFilter.Any.withSubtype(Subtype.SAMURAI),
                EffectTarget.TriggeringEntity
            ),
            then = Effects.PutOntoBattlefieldFromGraveyard(
                EffectTarget.TriggeringEntity,
                underYourControl = true
            )
        )
        description = "Whenever equipped creature dies, return that card to the battlefield under " +
            "your control if it's a Samurai card."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Exile(EffectTarget.EquippedCreature)
        description = "When Oathkeeper is put into a graveyard from the battlefield, exile equipped creature."
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "265"
        artist = "Arnie Swekel"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01b7c8e4-db88-48f3-bfd5-8d990f449b1c.jpg?1783944277"
        ruling(
            "2004-12-01",
            "Oathkeeper's second ability checks whether the card's creature type is Samurai, not " +
                "whether the creature was a Samurai when it left the battlefield."
        )
    }
}
