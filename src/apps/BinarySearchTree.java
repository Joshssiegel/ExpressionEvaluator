package apps;

public class BinarySearchTree<G extends Comparable<G>> {

	BSTNode<G> root;
	
	public boolean search(G target)
	{
		if(root==null)
			return false;
		BSTNode<G> tmp=root;
		while(tmp!=null)
		{
			int c=tmp.data.compareTo(target);
			if(c==0)
				return true;
			if(c<0)
			{
				tmp=tmp.right;
			}
			else
				tmp=tmp.left;
		}
		return false;
	}
	
}
