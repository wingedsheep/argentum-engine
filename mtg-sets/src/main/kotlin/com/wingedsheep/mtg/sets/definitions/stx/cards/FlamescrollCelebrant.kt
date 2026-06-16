package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Flamescroll Celebrant // Revel in Silence
 *
 * Modal double-faced card (CR 712). Cast either face from hand, never both.
 *
 * Front — Flamescroll Celebrant ({1}{R}, Creature — Human Shaman, 2/1):
 *   "Whenever an opponent activates an ability that isn't a mana ability, this creature deals
 *    1 damage to that player."  (the engine emits AbilityActivatedEvent only for non-mana
 *    abilities, since mana abilities resolve without the stack — loyalty abilities qualify.)
 *   "{1}{R}: This creature gets +2/+0 until end of turn."
 *
 * Back — Revel in Silence ({W}{W}, Instant):
 *   "Your opponents can't cast spells or activate planeswalkers' loyalty abilities this turn.
 *    Exile Revel in Silence."
 *   (Per Scryfall ruling, this is not countermagic — casting it in response to a spell or ability
 *    doesn't affect that spell or ability.)
 */
val FlamescrollCelebrant = card("Flamescroll Celebrant") {
    manaCost = "{1}{R}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Shaman"
    oracleText = "Whenever an opponent activates an ability that isn't a mana ability, " +
        "this creature deals 1 damage to that player.\n" +
        "{1}{R}: This creature gets +2/+0 until end of turn."
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.OpponentActivatesAbility
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    modalBack("Revel in Silence") {
        manaCost = "{W}{W}"
        typeLine = "Instant"
        oracleText = "Your opponents can't cast spells or activate planeswalkers' loyalty " +
            "abilities this turn.\nExile Revel in Silence."
        imageUri = "https://cards.scryfall.io/normal/back/0/d/0dba25e3-2b4f-45d4-965f-3834bcb359ee.jpg?1739656768"
        spell {
            selfExile()
            effect = Effects.Composite(
                Effects.CantCastSpells(EffectTarget.PlayerRef(Player.EachOpponent)),
                Effects.CantActivateLoyaltyAbilities(EffectTarget.PlayerRef(Player.EachOpponent)),
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "150"
        artist = "Uriah Voth"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0dba25e3-2b4f-45d4-965f-3834bcb359ee.jpg?1739656768"
        ruling("4/16/2021", "An activated ability that costs mana to activate is not a “mana ability” unless it could also produce mana and has no targets.")
        ruling("4/16/2021", "Loyalty abilities of planeswalkers are activated abilities, so activating one causes Flamescroll Celebrant's ability to trigger.")
        ruling("4/16/2021", "Casting Revel in Silence in response to a spell or ability won't affect that spell or ability. That is, it's not countermagic.")
    }
}
