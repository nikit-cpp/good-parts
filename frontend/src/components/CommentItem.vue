<template>
    <div class="comment">
        <div class="comment-head" :id="commentDTO.id">
            <a :href="'#'+this.commentDTO.id">#</a>
            <owner :owner="commentDTO.owner" :createTime="createDateTime" :editTime="editDateTime" :hideWrittenBy="true"></owner>
            <span v-if="!isEditing" class="comment-manage-buttons">
                <img class="edit-container-pen" src="../assets/pen.png" v-if="commentDTO.canEdit" @click="setEdit()"/>
                <img class="remove-container-x" src="../assets/remove.png" v-if="commentDTO.canDelete" @click="openDeleteConfirmation(commentDTO.id)"/>
            </span>
        </div>

        <div v-if="!isEditing" class="comment-content" v-html="commentDTO.text"></div>
        <comment-edit v-else :commentDTO="commentDTO"></comment-edit>
        <hr size="1"/>
    </div>
</template>

<script>
    import CommentEdit from './CommentEdit.vue'
    import bus, {COMMENT_CANCELED, COMMENT_UPDATED, COMMENT_DELETED} from '../bus'
    import {getPostId, getTimestampFromUtc} from '../utils'
    import Owner from './Owner.vue'
    import {DIALOG} from '../constants'

    export default {
        name: 'comment-item',
        props: ['commentDTO'], // it may be an object, for ability to set default values
        data() {
            return {
                isEditing: false,
            }
        },
        computed:{
            createDateTime(){
                return this.commentDTO.createDateTime ? getTimestampFromUtc(this.commentDTO.createDateTime) : null;
            },
            editDateTime(){
                return this.commentDTO.editDateTime ? getTimestampFromUtc(this.commentDTO.editDateTime) : null;
            }
        },
        methods: {
            setEdit(){
                this.isEditing = true;
            },
            resetEdit(){
                this.isEditing = false;
            },
            doDelete(){
                const commentId = this.commentDTO.id;
                this.$http.delete(`/api/post/${getPostId(this)}/comment/${commentId}`)
                    .then(successResponse => {
                        bus.$emit(COMMENT_DELETED, successResponse.body);
                    }, failResponce => {
                        console.error(failResponce);
                    })
            },
            openDeleteConfirmation(id){
                this.$modal.show(DIALOG, {
                    title: 'Comment delete confirmation',
                    text: 'Do you want to delete this comment #' + id +'?',
                    buttons: [
                        {
                            title: 'No',
                            default: true,
                            handler: () => {
                                this.$modal.hide(DIALOG)
                            }
                        },
                        {
                            title: 'Yes',
                            handler: () => {
                                this.doDelete();
                                this.$modal.hide(DIALOG)
                            }
                        },
                    ]
                })
            },

        },
        components:{
            CommentEdit,
            Owner
        },
        created(){
            bus.$on(COMMENT_CANCELED, this.resetEdit);
            bus.$on(COMMENT_UPDATED, this.resetEdit);
        },
        destroyed(){
            bus.$off(COMMENT_CANCELED, this.resetEdit);
            bus.$off(COMMENT_UPDATED, this.resetEdit);
        },
    };
</script>

<style lang="stylus">
    .comment {
        // border-width 1px
        // border-color black
        // border-style solid
        margin 2px;

        &-head{
            display flex
            flex-direction row
            flex-wrap wrap
        }

        .user-info {
            flex-grow 9
        }

        &-content {
            margin-top 0.2em
            margin-left 0.8em
            margin-right 0.8em
        }

        a{
            display flex
            flex-direction row
            align-items center
        }

        .comment-manage-buttons {
            //flex-grow 1
            display flex
            flex-direction row
            align-items center
            justify-content end

            img {
                margin 0 0.25em
            }

            img.edit-container-pen {
                height 16px;
                cursor pointer
            }
            img.remove-container-x {
                height 16px;
                cursor pointer
            }
            img.remove-container-x:hover {
                transition: 0.2s all;
                box-shadow: 0 0 2em red;
            }
        }

        hr {
            color lightgrey
        }
    }
</style>
